package ru.mirea.petgo.controller;

import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.service.ExportService;

import java.util.Scanner;

public class ExportController {
    private final Scanner scanner;
    private final ExportService exportService;

    public ExportController(Scanner scanner, ExportService exportService) {
        this.scanner = scanner;
        this.exportService = exportService;
    }

    public void showMenu() {
        boolean running = true;
        while (running) {
            System.out.println("""
            ========= ЭКСПОРТ ДАННЫХ =========
            1. Экспорт заявок в Excel (.xlsx)
            2. Экспорт заявок в CSV (.csv)
            3. Экспорт в оба формата
            0. Назад
            ================================================
            Выберите действие: 
            
            """);
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> exportToExcel();
                    case "2" -> exportToCsv();
                    case "3" -> exportToBoth();
                    case "0" -> running = false;
                    default -> System.out.println("Ошибка: неверный пункт меню.");
                }
            } catch (DatabaseException e) {
                System.out.println("Ошибка экспорта: " + e.getMessage());
            }
        }
    }

    private void exportToExcel() throws DatabaseException {
        String path = readPath("Введите путь к файлу (например, petgo_requests.xlsx): ");
        int count = exportService.exportWalkRequestsToExcel(path);
        System.out.println("Excel-файл создан: " + path + " (" + count + " строк)");
    }

    private void exportToCsv() throws DatabaseException {
        String path = readPath("Введите путь к файлу (например, petgo_requests.csv): ");
        int count = exportService.exportWalkRequestsToCsv(path);
        System.out.println("CSV-файл создан: " + path + " (" + count + " строк)");
    }

    private void exportToBoth() throws DatabaseException {
        String base = readPath("Введите базовое имя файла (без расширения): ");
        int xlsxCount = exportService.exportWalkRequestsToExcel(base + ".xlsx");
        int csvCount = exportService.exportWalkRequestsToCsv(base + ".csv");
        System.out.println("Excel: " + base + ".xlsx (" + xlsxCount + " строк)");
        System.out.println("CSV: " + base + ".csv (" + csvCount + " строк)");
    }

    private String readPath(String prompt) {
        System.out.print(prompt);
        String path = scanner.nextLine().trim();
        if (path.isEmpty()) {
            path = "petgo_requests";  // дефолт
            System.out.println("(использовано имя по умолчанию: " + path + ")");
        }
        return path;
    }
}
