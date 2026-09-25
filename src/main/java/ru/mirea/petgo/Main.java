package ru.mirea.petgo;

import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.model.Pet;
import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.model.enums.WalkStatus;
import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.UserRepository;
import ru.mirea.petgo.repository.WalkHistoryRepository;
import ru.mirea.petgo.repository.WalkRequestRepository;
import ru.mirea.petgo.service.WalkHistoryService;
import ru.mirea.petgo.service.WalkRequestService;
import ru.mirea.petgo.util.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("Подключение к БД работает");
            System.out.println();

            WalkRequestRepository walkRequestRepo = new WalkRequestRepository(conn);
            PetRepository petRepo = new PetRepository(conn);
            UserRepository userRepo = new UserRepository(conn);
            WalkHistoryRepository walkHistoryRepo = new WalkHistoryRepository(conn);

            WalkRequestService walkRequestService =
                    new WalkRequestService(walkRequestRepo, petRepo, userRepo);
            WalkHistoryService walkHistoryService =
                    new WalkHistoryService(walkHistoryRepo, walkRequestRepo);

            System.out.println("Поиск по имени питомца");
            try {
                List<WalkRequest> byName = walkRequestService.findByPetName("баРСик");
                System.out.println("Найдено: " + byName.size());
                for (WalkRequest r : byName) {
                    System.out.println("заявка " + r.getId()
                            + ": питомец " + getPetName(petRepo, r.getPetId())
                            + ", статус " + r.getStatus());
                }
            } catch (BusinessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (DatabaseException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
            }
            System.out.println();

            System.out.println("Поиск по дате");
            try {
                List<WalkRequest> byDate = walkRequestService.findByDate(LocalDate.of(2026, 9, 22));
                System.out.println("Найдено: " + byDate.size());
                for (WalkRequest r : byDate) {
                    System.out.println("заявка " + r.getId()
                            + ": питомец " + getPetName(petRepo, r.getPetId())
                            + ", дата " + r.getWalkDateTime());
                }
            } catch (BusinessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (DatabaseException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
            }
            System.out.println();

            System.out.println("Фильтр по статусу CREATED");
            try {
                List<WalkRequest> byStatus =
                        walkRequestService.findByStatus(WalkStatus.PENDING);
                System.out.println("Найдено: " + byStatus.size());
                for (WalkRequest r : byStatus) {
                    System.out.println("заявка " + r.getId()
                            + ": питомец " + getPetName(petRepo, r.getPetId())
                            + ", статус " + r.getStatus());
                }
            } catch (DatabaseException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
            }
            System.out.println();

            System.out.println("Фильтр по владельцу");
            try {
                List<WalkRequest> byOwner = walkRequestService.findByOwnerId(1);
                System.out.println("Найдено: " + byOwner.size());
                for (WalkRequest r : byOwner) {
                    System.out.println("заявка " + r.getId()
                            + ": питомец " + getPetName(petRepo, r.getPetId())
                            + ", владелец " + r.getOwnerId());
                }
            } catch (DatabaseException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
            }
            System.out.println();

            System.out.println("Сортировка по цене");
            try {
                List<WalkRequest> sorted =
                        walkRequestService.findAllSorted("price", true);
                System.out.println("Всего: " + sorted.size());
                for (WalkRequest r : sorted) {
                    System.out.println("заявка " + r.getId()
                            + ": питомец " + getPetName(petRepo, r.getPetId())
                            + ", цена " + r.getPrice()
                            + ", статус " + r.getStatus());
                }
            } catch (BusinessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (DatabaseException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
            }
            System.out.println();

            System.out.println("Сортировка по имени питомца");
            try {
                List<WalkRequest> sortedByPet =
                        walkRequestService.findAllSortedByPetName(true);
                System.out.println("Всего: " + sortedByPet.size());
                for (WalkRequest r : sortedByPet) {
                    System.out.println("заявка " + r.getId()
                            + ": питомец " + getPetName(petRepo, r.getPetId())
                            + ", статус " + r.getStatus());
                }
            } catch (DatabaseException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
            }
            System.out.println();

            System.out.println("Сортировка по неверному полю");
            try {
                walkRequestService.findAllSorted("wrong_field", true);
                System.out.println("Ошибки не было");
            } catch (BusinessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (DatabaseException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
            }
        }
    }

    private static String getPetName(PetRepository petRepo, int petId) {
        try {
            Pet pet = petRepo.findById(petId);
            if (pet != null) {
                return pet.getName();
            }
        } catch (SQLException e) {
            return "ошибка БД";
        }
        return "неизвестно";
    }
}