package ru.mirea.petgo.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petgo.domain.Tag;
import ru.mirea.petgo.domain.WalkRequest;
import ru.mirea.petgo.dto.TagCreateDto;
import ru.mirea.petgo.dto.TagDto;
import ru.mirea.petgo.error.ConflictException;
import ru.mirea.petgo.error.ResourceNotFoundException;
import ru.mirea.petgo.repository.TagRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final WalkRequestRepository requestRepository;

    public TagService(TagRepository tagRepository, WalkRequestRepository requestRepository) {
        this.tagRepository = tagRepository;
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public List<TagDto> findAll() {
        return tagRepository.findAll(Sort.by("name")).stream().map(TagDto::from).toList();
    }

    @Transactional
    public TagDto create(TagCreateDto dto) {
        String name = dto.name().strip();
        if (tagRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Тег «" + name + "» уже существует");
        }
        Tag tag = new Tag();
        tag.setName(name);
        tag.setColor(dto.color());
        return TagDto.from(tagRepository.save(tag));
    }

    @Transactional
    public TagDto update(UUID id, TagCreateDto dto) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Тег не найден"));
        String name = dto.name().strip();
        if (tagRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Тег «" + name + "» уже существует");
        }
        tag.setName(name);
        tag.setColor(dto.color());
        return TagDto.from(tag);
    }

    /** Связи тега с заявками удаляет БД (ON DELETE CASCADE). */
    @Transactional
    public void delete(UUID id) {
        if (!tagRepository.existsById(id)) {
            throw new ResourceNotFoundException("Тег не найден");
        }
        tagRepository.deleteById(id);
    }

    @Transactional
    public List<TagDto> attach(UUID requestId, UUID tagId) {
        WalkRequest request = findRequest(requestId);
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Тег не найден"));
        request.getTags().add(tag);          // повторная привязка безопасна
        return toDtos(request);
    }

    @Transactional
    public List<TagDto> detach(UUID requestId, UUID tagId) {
        WalkRequest request = findRequest(requestId);
        request.getTags().removeIf(t -> t.getId().equals(tagId));
        return toDtos(request);
    }

    private WalkRequest findRequest(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
    }

    private List<TagDto> toDtos(WalkRequest request) {
        return request.getTags().stream()
                .map(TagDto::from)
                .sorted(Comparator.comparing(TagDto::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
