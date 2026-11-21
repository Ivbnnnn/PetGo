package ru.mirea.petgo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import ru.mirea.petgo.domain.Pet;
import ru.mirea.petgo.domain.User;
import ru.mirea.petgo.domain.WalkRequest;
import ru.mirea.petgo.dto.ImportMode;
import ru.mirea.petgo.dto.ImportReportDto;
import ru.mirea.petgo.error.BadRequestException;
import ru.mirea.petgo.error.PayloadTooLargeException;
import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.TagRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CsvImportServiceTest {

    private static final String HEADER =
            "external_id;owner_email;pet_name;scheduled_at;duration_minutes;address;price";
    private static final String VALID_ROW =
            "EXT-1;owner@mail.ru;Барсик;2026-10-15 10:00;60;Москва, ул. Ленина 1;500,00";

    private final WalkRequestRepository requestRepository = mock(WalkRequestRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PetRepository petRepository = mock(PetRepository.class);
    private final TagRepository tagRepository = mock(TagRepository.class);
    private CsvImportService service;

    @BeforeEach
    void setUp() {
        service = new CsvImportService(requestRepository, userRepository, petRepository, tagRepository,
                mock(AuditService.class));
        ReflectionTestUtils.setField(service, "maxBytes", 1_000_000L);
        ReflectionTestUtils.setField(service, "maxRows", 100);

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@mail.ru");
        Pet pet = new Pet();
        pet.setId(UUID.randomUUID());
        pet.setName("Барсик");

        when(userRepository.findByEmailIgnoreCase("owner@mail.ru")).thenReturn(Optional.of(owner));
        when(petRepository.findFirstByOwner_IdAndNameIgnoreCase(owner.getId(), "Барсик"))
                .thenReturn(Optional.of(pet));
        when(requestRepository.findByExternalIdIn(any())).thenReturn(List.of());
    }

    private static MockMultipartFile csv(String... rows) {
        String body = "\uFEFF" + HEADER + "\n" + String.join("\n", rows);
        return new MockMultipartFile("file", "import.csv", "text/csv", body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void previewValidatesButDoesNotSave() {
        ImportReportDto report = service.preview(csv(VALID_ROW), ImportMode.ONLY_NEW);

        assertEquals(1, report.totalRows());
        assertEquals(1, report.validRows());
        assertEquals(0, report.errorRows());
        assertEquals(1, report.toCreate());
        assertFalse(report.applied());
        verify(requestRepository, never()).saveAll(any());
    }

    @Test
    void applySavesValidRowsAndReportsInvalidOnes() {
        String badRow = "EXT-2;ghost@mail.ru;Барсик;2026-10-15 10:00;60;Москва;500";

        ImportReportDto report = service.apply(csv(VALID_ROW, badRow), ImportMode.ONLY_NEW);

        assertTrue(report.applied());
        assertEquals(1, report.toCreate());
        assertEquals(1, report.errorRows());
        assertEquals(3, report.errors().get(0).line());
        verify(requestRepository, times(1)).saveAll(any());
    }

    @Test
    void onlyNewSkipsExistingAndUpdateModeUpdatesThem() {
        WalkRequest existing = new WalkRequest();
        existing.setExternalId("EXT-1");
        when(requestRepository.findByExternalIdIn(any())).thenReturn(List.of(existing));

        ImportReportDto onlyNew = service.preview(csv(VALID_ROW), ImportMode.ONLY_NEW);
        assertEquals(1, onlyNew.skipped());
        assertEquals(0, onlyNew.toCreate());

        ImportReportDto update = service.preview(csv(VALID_ROW), ImportMode.UPDATE_BY_EXTERNAL_ID);
        assertEquals(1, update.toUpdate());
        assertEquals(0, update.skipped());
    }

    @Test
    void duplicateExternalIdInsideFileIsAnError() {
        ImportReportDto report = service.preview(csv(VALID_ROW, VALID_ROW), ImportMode.ONLY_NEW);

        assertEquals(1, report.validRows());
        assertEquals(1, report.errorRows());
    }

    @Test
    void fileNotInUtf8IsRejected() {
        MockMultipartFile file = new MockMultipartFile("file", "bad.csv", "text/csv",
                new byte[]{(byte) 0xFF, (byte) 0xFE, 'a', ';', 'b'});
        assertThrows(BadRequestException.class, () -> service.preview(file, ImportMode.ONLY_NEW));
    }

    @Test
    void wrongExtensionIsRejected() {
        MockMultipartFile file = new MockMultipartFile("file", "data.txt", "text/plain",
                (HEADER + "\n" + VALID_ROW).getBytes(StandardCharsets.UTF_8));
        assertThrows(BadRequestException.class, () -> service.preview(file, ImportMode.ONLY_NEW));
    }

    @Test
    void tooLargeFileIsRejected() {
        ReflectionTestUtils.setField(service, "maxBytes", 10L);
        assertThrows(PayloadTooLargeException.class, () -> service.preview(csv(VALID_ROW), ImportMode.ONLY_NEW));
    }

    @Test
    void missingRequiredColumnIsRejected() {
        MockMultipartFile file = new MockMultipartFile("file", "import.csv", "text/csv",
                "external_id;owner_email\nEXT-1;owner@mail.ru".getBytes(StandardCharsets.UTF_8));
        assertThrows(BadRequestException.class, () -> service.preview(file, ImportMode.ONLY_NEW));
    }
}
