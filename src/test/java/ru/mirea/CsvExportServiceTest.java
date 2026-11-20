package ru.mirea.petgo.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.petgo.domain.Pet;
import ru.mirea.petgo.domain.RequestStatus;
import ru.mirea.petgo.domain.User;
import ru.mirea.petgo.domain.WalkRequest;
import ru.mirea.petgo.repository.WalkRequestRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CsvExportServiceTest {

    @Test
    void safeEscapesFormulaCharacters() {
        assertEquals("'=SUM(A1)", CsvExportService.safe("=SUM(A1)"));
        assertEquals("'+1", CsvExportService.safe("+1"));
        assertEquals("'-1", CsvExportService.safe("-1"));
        assertEquals("'@cmd", CsvExportService.safe("@cmd"));
    }

    @Test
    void safeKeepsOrdinaryTextAndHandlesNullAndNewLines() {
        assertEquals("Обычный текст", CsvExportService.safe("Обычный текст"));
        assertEquals("", CsvExportService.safe(null));
        assertEquals("a b", CsvExportService.safe("a\nb"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportStartsWithBomAndContainsHeaderAndRow() {
        WalkRequestRepository repository = mock(WalkRequestRepository.class);

        Pet pet = new Pet();
        pet.setName("Барсик");
        User owner = new User();
        owner.setEmail("owner@mail.ru");
        owner.setUsername("Иван");

        WalkRequest request = new WalkRequest();
        request.setExternalId("EXT-1");
        request.setPet(pet);
        request.setOwner(owner);
        request.setStatus(RequestStatus.CREATED);
        request.setScheduledAt(LocalDateTime.of(2026, 10, 15, 10, 0));
        request.setDurationMinutes(60);
        request.setAddress("Москва");
        request.setPrice(500.0);

        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(request)));

        Specification<WalkRequest> everything = (root, query, cb) -> null;
        byte[] bytes = new CsvExportService(repository).export(everything);

        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);

        String text = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(text.contains("external_id;status;priority;scheduled_at"));
        assertTrue(text.contains("EXT-1;CREATED;;2026-10-15 10:00;60;Москва;500,00;"));
        assertTrue(text.contains("Барсик;owner@mail.ru;Иван"));
    }
}
