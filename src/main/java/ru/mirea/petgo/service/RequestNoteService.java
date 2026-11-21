
package ru.mirea.petgo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.mirea.petgo.domain.*;
import ru.mirea.petgo.dto.RequestNoteDto;
import ru.mirea.petgo.error.*;
import ru.mirea.petgo.repository.*;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestNoteService {

    private final RequestNoteRepository notes;
    private final WalkRequestRepository requests;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<RequestNoteDto> list(UUID requestId) {
        if (!requests.existsById(requestId)) {
            throw new ResourceNotFoundException(
                    "Заявка не найдена"
            );
        }

        return notes
                .findByWalkRequest_IdOrderByPinnedDescCreatedAtDesc(
                        requestId
                )
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public RequestNoteDto create(
            UUID requestId, RequestNoteDto dto
    ) {
        validate(dto.content());

        WalkRequest request = requests.findById(requestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Заявка не найдена")
                );

        if (Boolean.TRUE.equals(request.getIsArchived())) {
            throw new ConflictException(
                    "Нельзя добавлять заметки к архивной заявке"
            );
        }

        User author = users.findById(currentUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Пользователь не найден"
                        ));

        RequestNote note = new RequestNote();
        note.setWalkRequest(request);
        note.setAuthor(author);
        note.setContent(dto.content().strip());

        return toDto(notes.saveAndFlush(note));
    }

    @Transactional
    public RequestNoteDto update(UUID id, RequestNoteDto dto) {
        RequestNote note = findEditable(id);

        validate(dto.content());
        note.setContent(dto.content().strip());

        return toDto(notes.saveAndFlush(note));
    }

    @Transactional
    public void delete(UUID id) {
        RequestNote note = findEditable(id);
        notes.delete(note);
    }

    @Transactional
    public RequestNoteDto pin(UUID id, boolean value) {
        RequestNote note = findEditable(id);
        note.setPinned(value);

        return toDto(notes.saveAndFlush(note));
    }

    private RequestNote findEditable(UUID id) {
        RequestNote note = notes.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Заметка не найдена"
                        ));

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        boolean admin = auth != null &&
                auth.getAuthorities().stream()
                        .anyMatch(a ->
                                a.getAuthority().equals("ROLE_ADMIN")
                        );

        if (!admin &&
                !note.getAuthor().getId().equals(currentUserId())) {
            throw new org.springframework.security
                    .access.AccessDeniedException(
                            "Недостаточно прав для изменения заметки"
                    );
        }

        return note;
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new org.springframework.security
                    .access.AccessDeniedException(
                            "Требуется авторизация"
                    );
        }

        return UUID.fromString(jwt.getSubject());
    }

    private void validate(String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException(
                    "Текст заметки не может быть пустым"
            );
        }

        if (content.length() > 5000) {
            throw new BadRequestException(
                    "Текст заметки слишком длинный"
            );
        }
    }

    private RequestNoteDto toDto(RequestNote note) {
        return new RequestNoteDto(
                note.getId(),
                note.getWalkRequest().getId(),
                note.getAuthor().getId(),
                note.getContent(),
                note.isPinned(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
