package ru.mirea.petgo.controller;

import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.service.DatabaseService;

import java.util.List;
import java.util.Scanner;

public class DatabaseController {
    private final Scanner scanner;
    private final DatabaseService databaseService;

    public DatabaseController(Scanner scanner, DatabaseService databaseService) {
        this.scanner = scanner;
        this.databaseService = databaseService;
    }

    public void showMenu() {
        boolean running = true;
        while (running) {
            MainController.DatabaseMenu();
            String choice = scanner.nextLine().trim();
        }

        try {
            switch (choice) {
                case "1" -> showAllTables();
                case "2" -> showOneTable();
                case "0" -> running = false;
                default -> System.out.println("Ошибка: неверный пункт меню.");
            }
        }
        catch (DatabaseException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    private void showAllTables() throws DatabaseException {
        List<String> tables = databaseService.getTableNames();
        for (String table: tables) {
            System.out.println();
            System.out.println("=== Таблица: " + table + " (" + databaseService.getRowCount(table) + " строк) ===");
            printTable(databaseService.getTableData(table));
        }
    }

    private void showOneTable() throws DatabaseException {
        List<String> tables = databaseService.getTableNames();
        System.out.println("Доступные таблицы:");
        for (int i = 0; i < tables.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + tables.get(i));
        }
        System.out.print("Выберите номер таблицы (или 0 — назад): ");

        String input = scanner.nextLine().trim();
        int idx;
        try {
            idx = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: введите целое число.");
            return;
        }
        if (idx == 0) return;
        if (idx < 1 || idx > tables.size()) {
            System.out.println("Ошибка: неверный номер.");
            return;
        }

        String table = tables.get(idx - 1);
        System.out.println("=== Таблица: " + table + " (" + databaseService.getRowCount(table) + " строк) ===");
        printTable(databaseService.getTableData(table));
    }

    private void printTable(DatabaseService.TableData data) {
        String[] headers = data.getHeaders();
        List<String[]> rows = data.getRows();

        if (rows.isEmpty()) {
            System.out.println("(таблица пуста)");
            return;
        }

        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }

        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }

        StringBuilder sep = new StringBuilder();
        for (int w : widths) {
            sep.append("-".repeat(w+2)).append("+");
        }
        System.out.println(sep);

        StringBuilder head = new StringBuilder();
        for (int i = 0; i < headers.length; i++) {
            head.append(" ").append(padRight(headers[i], widths[i])).append(" |");
        }
        System.out.println(head);
        System.out.println(sep);

        for (String[] row : rows) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < row.length; i++) {
                line.append(" ").append(padRight(row[i], widths[i])).append(" |");
            }
            System.out.println(line);
        }
        System.out.println(sep);
        System.out.println("Всего строк: " + rows.size());
    }

    private String padRight(String s, int width) {
        if (s == null) s = "—";
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }
}
