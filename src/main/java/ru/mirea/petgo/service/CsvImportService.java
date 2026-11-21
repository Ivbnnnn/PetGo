package ru.mirea.petgo.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.petgo.domain.Pet;
import ru.mirea.petgo.domain.RequestPriority;
import ru.mirea.petgo.domain.RequestStatus;
import ru.mirea.petgo.domain.Tag;
import ru.mirea.petgo.domain.User;
import ru.mirea.petgo.domain.WalkRequest;
import ru.mirea.petgo.dto.ImportMode;
import ru.mirea.petgo.dto.ImportReportDto;
import ru.mirea.petgo.dto.ImportRowError;
import ru.mirea.petgo.error.BadRequestException;
import ru.mirea.petgo.error.PayloadTooLargeException;
import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.TagRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Импорт заявок из CSV в два этапа.
 * preview: разбор и проверка без записи в БД (транзакция только для чтения).
 * apply: файл разбирается заново, корректные строки сохраняются, ошибочные пропускаются и описываются в отчёте.
 *
 * Статус и приоритет из файла не импортируются: статус меняется только по правилам переходов,
 * приоритет рассчитывается сервисом заявок.
 */
@Service
public class CsvImportService {

    private static final DateTimeFormatter SPACE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final BigDecimal MAX_PRICE = new BigDecimal("99999999.99");
    private static final List<String> REQUIRED = List.of(
            "external_id", "owner_email", "pet_name", "scheduled_at", "duration_minutes", "address", "price");

    private final WalkRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final TagRepository tagRepository;

    @Value("${app.csv.import-max-bytes:2097152}")
    private long maxBytes;

    @Value("${app.csv.import-max-rows:5000}")
    private int maxRows;

    public CsvImportService(WalkRequestRepository requestRepository,
                            UserRepository userRepository,
                            PetRepository petRepository,
                            TagRepository tagRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public ImportReportDto preview(MultipartFile file, ImportMode mode) {
        return process(file, mode, false);
    }

    @Transactional
    public ImportReportDto apply(MultipartFile file, ImportMode mode) {
        return process(file, mode, true);
    }

    private record ParsedRow(String externalId, User owner, Pet pet, User walker, LocalDateTime scheduledAt,
                             int duration, String address, BigDecimal price, String description, Set<Tag> tags) {
    }

    /** Кэши поиска на время одного импорта: не делаем запрос на каждую строку файла. */
    private class Lookups {
        final Map<String, Optional<User>> users = new HashMap<>();
        final Map<String, Optional<Pet>> pets = new HashMap<>();
        final Map<String, Optional<Tag>> tags = new HashMap<>();
        final Map<String, Long> seenIds = new HashMap<>();

        User user(String email) {
            return users.computeIfAbsent(email, userRepository::findByEmailIgnoreCase).orElse(null);
        }

        Pet pet(User owner, String name) {
            return pets.computeIfAbsent(owner.getId() + "|" + name.toLowerCase(),
                    key -> petRepository.findFirstByOwner_IdAndNameIgnoreCase(owner.getId(), name)).orElse(null);
        }

        Tag tag(String name) {
            return tags.computeIfAbsent(name.toLowerCase(),
                    key -> tagRepository.findByNameIgnoreCase(name)).orElse(null);
        }
    }

    private ImportReportDto process(MultipartFile file, ImportMode mode, boolean apply) {
        List<CSVRecord> records = readRecords(file);
        Lookups lookups = new Lookups();
        List<ImportRowError> errors = new ArrayList<>();
        List<ParsedRow> valid = new ArrayList<>();

        for (CSVRecord rec : records) {
            long line = rec.getRecordNumber() + 1;                  // +1: строка заголовка
            List<String> problems = new ArrayList<>();
            ParsedRow row = parse(rec, line, lookups, problems);
            if (problems.isEmpty()) {
                valid.add(row);
            } else {
                errors.add(new ImportRowError(line, String.join("; ", problems)));
            }
        }

        Map<String, WalkRequest> existing = requestRepository
                .findByExternalIdIn(valid.stream().map(ParsedRow::externalId).toList()).stream()
                .collect(Collectors.toMap(WalkRequest::getExternalId, r -> r, (a, b) -> a));

        int toCreate = 0;
        int toUpdate = 0;
        int skipped = 0;
        List<WalkRequest> toSave = new ArrayList<>();
        for (ParsedRow row : valid) {
            WalkRequest found = existing.get(row.externalId());
            if (found == null) {
                toCreate++;
                if (apply) {
                    toSave.add(fill(new WalkRequest(), row, true));
                }
            } else if (mode == ImportMode.ONLY_NEW) {
                skipped++;
            } else {
                toUpdate++;
                if (apply) {
                    toSave.add(fill(found, row, false));
                }
            }
        }

        if (apply) {
            requestRepository.saveAll(toSave);
            // TODO: auditService — импорт: режим, создано, обновлено, пропущено, ошибок (без содержимого файла)
        }
        return new ImportReportDto(records.size(), valid.size(), errors.size(),
                toCreate, toUpdate, skipped, apply, errors);
    }

    private WalkRequest fill(WalkRequest request, ParsedRow row, boolean created) {
        request.setExternalId(row.externalId());
        request.setPet(row.pet());
        request.setOwner(row.owner());
        request.setWalker(row.walker());
        request.setScheduledAt(row.scheduledAt());
        request.setDurationMinutes(row.duration());
        request.setPriority(RequestPriority.forDuration(row.duration()));
        request.setAddress(row.address());
        request.setPrice(row.price().doubleValue());
        request.setDescription(row.description());
        if (row.tags() != null) {                       // пустая колонка tags: теги не трогаем
            request.getTags().clear();
            request.getTags().addAll(row.tags());
        }
        if (created) {
            request.setStatus(RequestStatus.CREATED);
            request.setIsArchived(false);
        }
        return request;
    }

    private ParsedRow parse(CSVRecord rec, long line, Lookups lookups, List<String> problems) {
        if (!rec.isConsistent()) {
            problems.add("число столбцов не совпадает с заголовком");
            return null;
        }

        String externalId = clean(rec.get("external_id"));
        if (externalId.isEmpty() || externalId.length() > 50) {
            problems.add("external_id: обязательное поле, не длиннее 50 символов");
        } else {
            Long first = lookups.seenIds.putIfAbsent(externalId, line);
            if (first != null) {
                problems.add("external_id повторяется в файле (первая строка " + first + ")");
            }
        }

        User owner = null;
        String ownerEmail = clean(rec.get("owner_email")).toLowerCase();
        if (ownerEmail.isEmpty()) {
            problems.add("owner_email: обязательное поле");
        } else {
            owner = lookups.user(ownerEmail);
            if (owner == null) {
                problems.add("владелец с email " + ownerEmail + " не найден");
            }
        }

        Pet pet = null;
        String petName = clean(rec.get("pet_name"));
        if (petName.isEmpty()) {
            problems.add("pet_name: обязательное поле");
        } else if (owner != null) {
            pet = lookups.pet(owner, petName);
            if (pet == null) {
                problems.add("питомец «" + petName + "» не найден у этого владельца");
            }
        }

        User walker = null;
        String walkerEmail = optional(rec, "walker_email").toLowerCase();
        if (!walkerEmail.isEmpty()) {
            walker = lookups.user(walkerEmail);
            if (walker == null) {
                problems.add("выгульщик с email " + walkerEmail + " не найден");
            }
            // TODO: когда у User появятся роль и признак активности, проверить, что walker активен и имеет роль WALKER
        }

        LocalDateTime scheduledAt = parseDate(clean(rec.get("scheduled_at")), problems);

        int duration = 0;
        try {
            duration = Integer.parseInt(clean(rec.get("duration_minutes")));
            if (duration < 1 || duration > 1440) {
                problems.add("duration_minutes: от 1 до 1440");
            }
        } catch (NumberFormatException e) {
            problems.add("duration_minutes: нужно целое число");
        }

        String address = clean(rec.get("address"));
        if (address.isEmpty() || address.length() > 200) {
            problems.add("address: обязательное поле, не длиннее 200 символов");
        }

        BigDecimal price = null;
        try {
            price = new BigDecimal(clean(rec.get("price")).replace(',', '.'));
            if (price.signum() < 0 || price.compareTo(MAX_PRICE) > 0) {
                problems.add("price: от 0 до 99999999.99");
            }
        } catch (NumberFormatException e) {
            problems.add("price: нужно число");
        }

        String description = optional(rec, "description");
        if (description.length() > 2000) {
            problems.add("description: не длиннее 2000 символов");
        }

        Set<Tag> tags = null;
        String tagsCell = optional(rec, "tags");
        if (!tagsCell.isEmpty()) {
            tags = new HashSet<>();
            for (String part : tagsCell.split("\\|")) {
                String name = clean(part);
                if (name.isEmpty()) {
                    continue;
                }
                Tag tag = lookups.tag(name);
                if (tag == null) {
                    problems.add("тег «" + name + "» не найден");
                } else {
                    tags.add(tag);
                }
            }
        }

        if (!problems.isEmpty()) {
            return null;
        }
        return new ParsedRow(externalId, owner, pet, walker, scheduledAt, duration,
                address, price, description, tags);
    }

    private static LocalDateTime parseDate(String value, List<String> problems) {
        if (value.isEmpty()) {
            problems.add("scheduled_at: обязательное поле");
            return null;
        }
        LocalDateTime result;
        try {
            result = LocalDateTime.parse(value);                         // 2026-10-15T10:00
        } catch (DateTimeParseException first) {
            try {
                result = LocalDateTime.parse(value, SPACE_FORMAT);       // 2026-10-15 10:00
            } catch (DateTimeParseException second) {
                problems.add("scheduled_at: формат 2026-10-15 10:00");
                return null;
            }
        }
        if (result.getYear() < 2000 || result.getYear() > 2100) {
            problems.add("scheduled_at: год вне диапазона 2000–2100");
        }
        return result;
    }

    private static String optional(CSVRecord rec, String column) {
        return rec.isSet(column) ? clean(rec.get(column)) : "";
    }

    /** Убирает пробелы и апостроф, который экспорт ставит перед = + - @ для защиты от формул. */
    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        String s = value.strip();
        if (s.length() > 1 && s.charAt(0) == '\'' && "=+-@\t".indexOf(s.charAt(1)) >= 0) {
            s = s.substring(1);
        }
        return s;
    }

    private List<CSVRecord> readRecords(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Файл не выбран или пустой");
        }
        if (file.getSize() > maxBytes) {
            throw new PayloadTooLargeException("Файл больше " + (maxBytes / 1024) + " КБ");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".csv")) {
            throw new BadRequestException("Нужен файл с расширением .csv");
        }

        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(file.getBytes()))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new BadRequestException("Файл должен быть в кодировке UTF-8");
        } catch (IOException e) {
            throw new BadRequestException("Не удалось прочитать файл");
        }
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();
        try (CSVParser parser = format.parse(new StringReader(text))) {
            for (String column : REQUIRED) {
                if (!parser.getHeaderMap().containsKey(column)) {
                    throw new BadRequestException("В файле нет колонки «" + column + "»");
                }
            }
            List<CSVRecord> list = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (list.size() >= maxRows) {
                    throw new BadRequestException("В файле больше " + maxRows + " строк");
                }
                list.add(record);
            }
            return list;
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException | UncheckedIOException | IllegalStateException e) {
            throw new BadRequestException("Файл не является корректным CSV");
        }
    }
}
