package ru.mirea.petgo.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.petgo.dto.ImportMode;
import ru.mirea.petgo.dto.ImportReportDto;
import ru.mirea.petgo.filter.WalkRequestFilter;
import ru.mirea.petgo.service.CsvExportService;
import ru.mirea.petgo.service.CsvImportService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/walk-requests")
public class WalkRequestCsvController {

    private final CsvExportService exportService;
    private final CsvImportService importService;

    public WalkRequestCsvController(CsvExportService exportService, CsvImportService importService) {
        this.exportService = exportService;
        this.importService = importService;
    }

    /** Экспорт с теми же параметрами фильтра, что и у списка заявок. */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(WalkRequestFilter filter) {
        byte[] body = exportService.export(filter.toSpecification());
        String fileName = "walk-requests-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportReportDto preview(@RequestParam("file") MultipartFile file,
                                   @RequestParam(defaultValue = "ONLY_NEW") ImportMode mode) {
        return importService.preview(file, mode);
    }

    @PostMapping(value = "/import/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportReportDto apply(@RequestParam("file") MultipartFile file,
                                 @RequestParam(defaultValue = "ONLY_NEW") ImportMode mode) {
        return importService.apply(file, mode);
    }
}
