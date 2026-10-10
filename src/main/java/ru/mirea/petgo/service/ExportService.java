package ru.mirea.petgo.service;

import ru.mirea.petgo.dto.WalkRequestRow;
import ru.mirea.petgo.exception.ExportException;
import ru.mirea.petgo.util.CsvExporter;
import ru.mirea.petgo.util.ExcelExporter;
import ru.mirea.petgo.util.Labels;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String[] HEADERS = {
            "Питомец", "Владелец", "Выгульщик", "Дата прогулки",
            "Длительность (мин)", "Адрес", "Статус", "Цена"
    };

    public int exportToCsv(List<WalkRequestRow> rows, File file) throws ExportException {
        try {
            CsvExporter.export(toTable(rows), HEADERS, file.getAbsolutePath());
        } catch (IOException e) {
            throw new ExportException("Не удалось сохранить файл: " + file.getName(), e);
        }
        return rows.size();
    }

    public int exportToExcel(List<WalkRequestRow> rows, File file) throws ExportException {
        try {
            ExcelExporter.export(toTable(rows), HEADERS, file.getAbsolutePath());
        } catch (IOException e) {
            throw new ExportException("Не удалось сохранить файл: " + file.getName(), e);
        }
        return rows.size();
    }

    private List<String[]> toTable(List<WalkRequestRow> rows) {
        List<String[]> table = new ArrayList<>();
        for (WalkRequestRow r : rows) {
            table.add(new String[] {
                    r.petName(),
                    r.ownerName(),
                    r.walkerName() == null ? "—" : r.walkerName(),
                    r.walkDateTime() == null ? "" : r.walkDateTime().format(DATE_FMT),
                    String.valueOf(r.durationMinutes()),
                    r.walkAddress() == null ? "" : r.walkAddress(),
                    Labels.status(r.status()),
                    r.price() == null ? "" : r.price().toPlainString()
            });
        }
        return table;
    }
}