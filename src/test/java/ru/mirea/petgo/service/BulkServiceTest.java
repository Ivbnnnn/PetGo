package ru.mirea.petgo.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import ru.mirea.petgo.domain.RequestStatus;
import ru.mirea.petgo.domain.Tag;
import ru.mirea.petgo.domain.WalkRequest;
import ru.mirea.petgo.dto.BulkAction;
import ru.mirea.petgo.dto.BulkRequestDto;
import ru.mirea.petgo.dto.BulkResponseDto;
import ru.mirea.petgo.error.BadRequestException;
import ru.mirea.petgo.repository.TagRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BulkServiceTest {

    private final WalkRequestRepository requestRepository = mock(WalkRequestRepository.class);
    private final TagRepository tagRepository = mock(TagRepository.class);
    private final WalkRequestService requestService = mock(WalkRequestService.class);
    private final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
    private final BulkService service = new BulkService(requestRepository, tagRepository, requestService, txManager,
            mock(AuditService.class));

    @Test
    void archiveReportsResultForEveryRequest() {
        UUID okId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        UUID archivedId = UUID.randomUUID();

        WalkRequest ok = new WalkRequest();
        ok.setId(okId);
        WalkRequest archived = new WalkRequest();
        archived.setId(archivedId);
        archived.setIsArchived(true);

        when(requestRepository.findById(okId)).thenReturn(Optional.of(ok));
        when(requestRepository.findById(missingId)).thenReturn(Optional.empty());
        when(requestRepository.findById(archivedId)).thenReturn(Optional.of(archived));

        BulkResponseDto response = service.execute(
                new BulkRequestDto(List.of(okId, missingId, archivedId), BulkAction.ARCHIVE, null, null));

        assertEquals(3, response.total());
        assertEquals(1, response.succeeded());
        assertEquals(2, response.failed());
        assertTrue(ok.getIsArchived());
        assertEquals("Заявка не найдена", response.results().get(1).error());
        assertEquals("Заявка уже в архиве", response.results().get(2).error());
    }

    @Test
    void changeStatusWithoutStatusIsRejected() {
        BulkRequestDto dto = new BulkRequestDto(List.of(UUID.randomUUID()), BulkAction.CHANGE_STATUS, null, null);
        assertThrows(BadRequestException.class, () -> service.execute(dto));
    }

    @Test
    void cancelDelegatesToWalkRequestService() {
        UUID id = UUID.randomUUID();
        WalkRequest request = new WalkRequest();
        request.setId(id);
        when(requestRepository.findById(id)).thenReturn(Optional.of(request));

        BulkResponseDto response = service.execute(
                new BulkRequestDto(List.of(id), BulkAction.CHANGE_STATUS, RequestStatus.CANCELLED, null));

        assertEquals(1, response.succeeded());
        verify(requestService).cancel(id);
    }

    @Test
    void secondAddOfSameTagFails() {
        UUID requestId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();
        WalkRequest request = new WalkRequest();
        request.setId(requestId);
        Tag tag = new Tag();
        tag.setId(tagId);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(tagRepository.existsById(tagId)).thenReturn(true);
        when(tagRepository.getReferenceById(tagId)).thenReturn(tag);

        BulkRequestDto dto = new BulkRequestDto(List.of(requestId), BulkAction.ADD_TAG, null, tagId);
        assertEquals(1, service.execute(dto).succeeded());

        BulkResponseDto second = service.execute(dto);
        assertEquals(0, second.succeeded());
        assertEquals("Тег уже добавлен", second.results().get(0).error());
    }
}
