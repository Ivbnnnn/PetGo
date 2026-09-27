package ru.mirea.petgo.controller;

import java.util.Scanner;
import java.util.List;

import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.service.WalkRequestService;

public class SortRequestController {
    private final Scanner scanner;
    private final WalkRequestService walkRequestService;

    public SortRequestController(Scanner scanner, WalkRequestService walkRequestService) {
        this.scanner = scanner;
        this.walkRequestService = walkRequestService;
    }

    private void sortByDate(){
        try{
            System.out.println("""
                1. По возрастанию
                2. По убыванию
            """);
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":{
                    List<WalkRequest> reqs = walkRequestService.findAllSorted("walk_datetime", true);
                    if (reqs.isEmpty()){
                        System.out.println("Ничего не найдено");
                        return;
                    }
                    for (WalkRequest req: reqs){
                        printRequest(req);
                    }
                    break;}
                case "2":{
                    List<WalkRequest> reqs = walkRequestService.findAllSorted("walk_datetime", false);
                    if (reqs.isEmpty()){
                        System.out.println("Ничего не найдено");
                        return;
                    }
                    for (WalkRequest req: reqs){
                        printRequest(req);
                    }}
                    break;
            
                default:
                    System.out.println("Такого варинта нет.");
            }
        } catch(DatabaseException e){
            System.out.println(e.getMessage());
        } catch(BusinessException e){
            System.out.println(e.getMessage());
        }
    }


    private void sortByDuration(){
        try{
            System.out.println("""
                1. По возрастанию
                2. По убыванию
            """);
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":{
                    List<WalkRequest> reqs = walkRequestService.findAllSorted("duration_minutes", true);
                    if (reqs.isEmpty()){
                        System.out.println("Ничего не найдено");
                        return;
                    }
                    for (WalkRequest req: reqs){
                        printRequest(req);
                    }
                    break;}
                case "2":{
                    List<WalkRequest> reqs = walkRequestService.findAllSorted("duration_minutes", false);
                    if (reqs.isEmpty()){
                        System.out.println("Ничего не найдено");
                        return;
                    }
                    for (WalkRequest req: reqs){
                        printRequest(req);
                    }}
                    break;
            
                default:
                    System.out.println("Такого варинта нет.");
            }
        } catch(DatabaseException e){
            System.out.println(e.getMessage());
        } catch(BusinessException e){
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
                    ========= СОРТИРОВКА ЗАЯВОК =========
                    1. По дате прогулки
                    2. По длительности
                    3. Назад
                    ========================================
                    """);

            System.out.print("Выберите действие: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    sortByDate();
                    break;
                case "2":
                    sortByDuration();
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
