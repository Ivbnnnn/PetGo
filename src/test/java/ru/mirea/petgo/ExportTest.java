package ru.mirea.petgo;

import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;
import ru.mirea.petgo.service.ExportService;
import ru.mirea.petgo.util.DatabaseManager;

import java.sql.Connection;

public class ExportTest {
    public static void main(String[] args) throws Exception {
        try (Connection conn = DatabaseManager.getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            PetRepository petRepo = new PetRepository(conn);
            WalkRequestRepository walkRepo = new WalkRequestRepository(conn);

            ExportService exportService = new ExportService(walkRepo, userRepo, petRepo);

            System.out.println("=== ЭКСПОРТ ===");
            int xlsxCount = exportService.exportWalkRequestsToExcel("petgo_requests.xlsx");
            System.out.println("✅ Excel: petgo_requests.xlsx (" + xlsxCount + " строк)");

            int csvCount = exportService.exportWalkRequestsToCsv("petgo_requests.csv");
            System.out.println("✅ CSV: petgo_requests.csv (" + csvCount + " строк)");
        }
    }
}