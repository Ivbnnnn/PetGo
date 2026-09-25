package ru.mirea.petgo.service;

import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.model.Pet;
import ru.mirea.petgo.model.User;
import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;
import ru.mirea.petgo.util.CsvExporter;
import ru.mirea.petgo.util.ExcelExporter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] HEADERS = {
            "ID", "Питомец", "Владелец", "Выгульщик",
            "Дата прогулки", "Длительность (мин)", "Адрес",
            "Статус", "Описание", "Цена", "Создана"
    };

    private final WalkRequestRepository walkRequestRepository;
    private final UserRepository userRepository;
    private final PetRepository petRepository;

    public ExportService(WalkRequestRepository walkRequestRepository, UserRepository userRepository,
            PetRepository petRepository) {
        this.walkRequestRepository = walkRequestRepository;
        this.userRepository = userRepository;
        this.petRepository = petRepository;
    }

    public int exportWalkRequestsToExcel(String filePath) throws DatabaseException {
        List<String[]> rows = buildRows();
        try {
            ExcelExporter.export(rows, HEADERS, filePath);
        } catch (IOException e) {
            throw new DatabaseException("Ошибка экспорта в Excel: " + filePath, e);
        }
        return rows.size();
    }

    public int exportWalkRequestsToCsv(String filePath) throws DatabaseException {
        List<String[]> rows = buildRows();
        try {
            CsvExporter.export(rows, HEADERS, filePath);
        } catch (IOException e) {
            throw new DatabaseException("Ошибка экспорта в CSV: " + filePath, e);
        }
        return rows.size();
    }

    private List<String[]> buildRows() throws DatabaseException {
        List<WalkRequest> requests;
        try {
            requests = walkRequestRepository.findAll();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка получения заявок для экспорта", e);
        }

        List<String[]> rows = new ArrayList<>();
        for (WalkRequest r : requests) {
            rows.add(new String[] {
                    String.valueOf(r.getId()),
                    resolvePetName(r.getPetId()),
                    resolveUserName(r.getOwnerId()),
                    r.getWalkerId() == null ? "—" : resolveUserName(r.getWalkerId()),
                    r.getWalkDateTime() == null ? "" : r.getWalkDateTime().format(DATE_FMT),
                    String.valueOf(r.getDurationMinutes()),
                    r.getWalkAddress() == null ? "" : r.getWalkAddress(),
                    r.getStatus() == null ? "" : r.getStatus().name(),
                    r.getDescription() == null ? "" : r.getDescription(),
                    r.getPrice() == null ? "" : r.getPrice().toPlainString(),
                    r.getCreatedAt() == null ? "" : r.getCreatedAt().format(DATE_FMT)
            });
        }
        return rows;
    }

    private String resolvePetName(int petId) {
        try {
            Pet pet = petRepository.findById(petId);
            return pet == null ? "—" : pet.getName();
        } catch (SQLException e) {
            return "—";
        }
    }

    private String resolveUserName(int userId) {
        try {
            User user = userRepository.findById(userId);
            return user == null ? "—" : user.getName();
        } catch (SQLException e) {
            return "—";
        }
    }
}