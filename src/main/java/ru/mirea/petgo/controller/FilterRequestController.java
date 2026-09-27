package ru.mirea.petgo.controller;

import java.util.Scanner;
import java.util.List;

import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.model.enums.WalkStatus;
import ru.mirea.petgo.service.WalkRequestService;

public class FilterRequestController {
    private final Scanner scanner;
    private final WalkRequestService walkRequestService;

    public FilterRequestController(Scanner scanner, WalkRequestService walkRequestService) {
        this.scanner = scanner;
        this.walkRequestService = walkRequestService;
    }

    private void filterByStatus(){
        try{
            System.out.print("Введите статус: ");
            WalkStatus status = WalkStatus.valueOf(scanner.nextLine().trim().toUpperCase());

            List<WalkRequest> reqs = walkRequestService.findByStatus(status);
            if (reqs.isEmpty()){
                System.out.println("Ничего не найдено");
                return;
            }
            for (WalkRequest req : reqs){
                printRequest(req);
            }
        } catch (IllegalArgumentException e){
            System.out.println("Нет такого статуса.");
        } catch (DatabaseException e){
            System.out.println(e.getMessage());
        }
    }
    private void filterByOwnerId(){
        try{
            System.out.print("Введите ownerId: ");
            Integer ownerId = Integer.parseInt(scanner.nextLine());

            List<WalkRequest> reqs = walkRequestService.findByOwnerId(ownerId);
            if (reqs.isEmpty()){
                System.out.println("Ничего не найдено");
                return;
            }
            for (WalkRequest req : reqs){
                printRequest(req);
            }
        } catch (NumberFormatException e){
            System.out.println("Введите корректное число.");
        } catch (DatabaseException e){
            System.out.println(e.getMessage());
        }
    }


    private void printRequest(WalkRequest walkRequest) {
        System.out.println(
                "ID: " + walkRequest.getId() +
                        ", petId: " + walkRequest.getPetId() +
                        ", ownerId: " + walkRequest.getOwnerId() +
                        ", walkerId: " + walkRequest.getWalkerId() +
                        ", время: " + walkRequest.getWalkDateTime() +
                        ", длительность: " + walkRequest.getDurationMinutes() +
                        ", адрес: " + walkRequest.getWalkAddress() +
                        ", статус: " + walkRequest.getStatus() +
                        ", описание: " + walkRequest.getDescription());
    }


    public void showMenu() {
        boolean running = true;

        while (running) {
            System.out.println("""
                    ========= ФИЛЬТРАЦИЯ ЗАЯВОК =========
                    1. По статусу
                    2. По ID владельца
                    3. Назад
                    ========================================
                    """);

            System.out.print("Выберите действие: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    filterByStatus();
                    break;
                case "2":
                    filterByOwnerId();
                    break;
                case "3":
                    running = false;
                    break;
                default:
                    System.out.println("Такого пункта нет.");
            }

        }
    }
}
