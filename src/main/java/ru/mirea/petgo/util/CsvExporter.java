package ru.mirea.petgo.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CsvExporter {

    public static void export(List<String[]> rows, String[] headers, String filePath)
            throws IOException {
        try (PrintWriter writer = new PrintWriter(filePath, StandardCharsets.UTF_8)) {
            writer.print('\uFEFF');
            writer.println(String.join(";", headers));

            for (String[] row : rows) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < row.length; i++) {
                    if (i > 0)
                        line.append(";");
                    line.append(escapeCsv(row[i]));
                }
                writer.println(line);
            }
        }
    }

    private static String escapeCsv(String value) {
        if (value == null)
            return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}