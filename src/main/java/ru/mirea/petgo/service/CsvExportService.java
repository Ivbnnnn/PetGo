package ru.mirea.petgo.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petgo.domain.Tag;
import ru.mirea.petgo.domain.WalkRequest;
import ru.mirea.petgo.error.BadRequestException;
import ru.mirea.petgo.repository.WalkRequestRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Экспорт заявок в CSV: кодировка UTF-8 с BOM, разделитель «;» (так открывает русский Excel).
 * Набор колонок задан явно, секретных данных в файле нет. Колонки совместимы с импортом.
 */
@Service
public class CsvExportService {

    private static final int PAGE_SIZE = 500;
    private static final int MAX_ROWS = 50_000;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    static final String[] HEADER = {
            "external_id", "status", "priority", "scheduled_at", "duration_minutes", "address", "price",
            "description", "is_archived", "pet_name", "owner_email", "owner_name",
            "walker_email", "walker_name", "tags"};

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setHeader(HEADER)
            .setRecordSeparator("\r\n")
            .get();

    private final WalkRequestRepository requestRepository;

    public CsvExportService(WalkRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public byte[] export(Specification<WalkRequest> spec) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            writer.write('\uFEFF');       // BOM пишем до создания printer: он сразу печатает заголовок
            try (CSVPrinter printer = new CSVPrinter(writer, FORMAT)) {
                Sort sort = Sort.by("scheduledAt").and(Sort.by("id"));     // стабильный порядок страниц
                int page = 0;
                int rows = 0;
                Page<WalkRequest> chunk;
                do {
                    chunk = requestRepository.findAll(spec, PageRequest.of(page++, PAGE_SIZE, sort));
                    for (WalkRequest r : chunk.getContent()) {
                        if (++rows > MAX_ROWS) {
                            throw new BadRequestException("Слишком много строк для экспорта, уточните фильтры");
                        }
                        printer.printRecord(toRow(r));
                    }
                } while (chunk.hasNext());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private Object[] toRow(WalkRequest r) {
        return new Object[] {
                safe(r.getExternalId()),
                r.getStatus() == null ? "" : r.getStatus().name(),
                r.getPriority() == null ? "" : r.getPriority().name(),
                r.getScheduledAt() == null ? "" : r.getScheduledAt().format(DATE_TIME),
                r.getDurationMinutes() == null ? "" : String.valueOf(r.getDurationMinutes()),
                safe(r.getAddress()),
                formatPrice(r.getPrice()),
                safe(r.getDescription()),
                String.valueOf(Boolean.TRUE.equals(r.getIsArchived())),
                safe(r.getPet() == null ? null : r.getPet().getName()),
                safe(r.getOwner() == null ? null : r.getOwner().getEmail()),
                safe(r.getOwner() == null ? null : r.getOwner().getUsername()),
                safe(r.getWalker() == null ? null : r.getWalker().getEmail()),
                safe(r.getWalker() == null ? null : r.getWalker().getUsername()),
                safe(r.getTags().stream().map(Tag::getName).sorted().collect(Collectors.joining("|")))
        };
    }

    /** Цена с запятой, чтобы русский Excel распознал число. Импорт принимает и запятую, и точку. */
    private static String formatPrice(Double price) {
        return price == null ? "" : String.format(Locale.US, "%.2f", price).replace('.', ',');
    }

    /** Защита от CSV-инъекции: ячейка, начинающаяся с = + - @, в Excel станет формулой. */
    static String safe(String value) {
        if (value == null) {
            return "";
        }
        String s = value.replace("\r", " ").replace("\n", " ");
        return !s.isEmpty() && "=+-@\t".indexOf(s.charAt(0)) >= 0 ? "'" + s : s;
    }
}
