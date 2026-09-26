package ru.mirea.petgo.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import org.apache.poi.hssf.record.cf.DataBarFormatting;

import java.util.List;
import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.EntityNotFoundException;
import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.model.enums.WalkStatus;
import ru.mirea.petgo.service.WalkRequestService;

public class WalkRequestController {

    private final Scanner scanner;
    private final WalkRequestService walkRequestService;

    public WalkRequestController(Scanner scanner, WalkRequestService walkRequestService) {
        this.scanner = scanner;
        this.walkRequestService = walkRequestService;
    }

    private void createRequest(){
        try{
            WalkRequest walkRequest = readRequestData(new WalkRequest());
            walkRequestService.create(walkRequest);
            System.out.println("Прогулка создана");
        } catch (BusinessException e){
            System.out.println(e.getMessage());
        } catch (DatabaseException e){
            System.out.println(e.getMessage());
        }
        
    }

    private void showAllRequests(){
        try{
            List<WalkRequest> requests = walkRequestService.findAll();
            if (requests.isEmpty()){
                System.out.println("Прогулок нет");
                return;
            }
            for (WalkRequest req : requests){
                printRequest(req);
            }
        } catch (DatabaseException e){
            System.out.println(e.getMessage());
        }
        
    }

    private void findRequestById(){
        try{
            System.out.print("Введите Id прогулки: ");
            WalkRequest req = walkRequestService.findById(Integer.parseInt(scanner.nextLine()));
            printRequest(req);
        } catch (EntityNotFoundException e){
            System.out.println(e.getMessage());
        } catch (DatabaseException e){
            System.out.println(e.getMessage());
        }
    }

    private void updateRequest(){
        try{
            System.out.print("Введите Id прогулки для обновления: ");
            WalkRequest req = walkRequestService.findById(Integer.parseInt(scanner.nextLine()));
            System.out.println("Введите новые данные");
            readRequestDataForUpdate(req);
            walkRequestService.update(req);
            System.out.println("Данные успешно обновлены");
        } catch (EntityNotFoundException e){
            System.out.println(e.getMessage());
        } catch (DatabaseException e){
            System.out.println(e.getMessage());
        } catch (BusinessException e){
            System.out.println(e.getMessage());
        }
    }

    private void deleteRequest(){
        try{
            System.out.print("Введите ID прогулки для удаления: ");
            walkRequestService.delete(Integer.parseInt(scanner.nextLine()));
            System.out.println("Заявка успешно удалена.");
        } catch (DatabaseException e){
            System.out.println(e.getMessage());
        } catch (EntityNotFoundException e){
            System.out.println(e.getMessage());
        }
    }

    // private void findRequest(){
    //     try{
    //         System.out.println("""
    //                 Выберите вариант поиска:
    //                 1.
    //                 2.
    //                 3. Выход
    //                 """);
    //         String choice = scanner.nextLine();
    //         switch (choice) {
    //             case "1":
                    
    //                 break;
            
    //             default:
    //                 System.out.println("Нет такого варинта");                   
    //         }
    //     }
    // }

    private void printRequest(WalkRequest walkRequest){
        System.out.println(
            "ID: " + walkRequest.getId() + 
            ", petId: "+ walkRequest.getPetId() + 
            ", ownerId: " + walkRequest.getOwnerId() +
            ", walkerId: " + walkRequest.getWalkerId() +
            ", время: " + walkRequest.getWalkDateTime() +
            ", длительность: " + walkRequest.getDurationMinutes() +
            ", адрес: " + walkRequest.getWalkAddress() +
            ", статус: " + walkRequest.getStatus() +
            ", описание: " + walkRequest.getDescription() 
        );
    }

    private WalkRequest readRequestData(WalkRequest walkRequest){
        System.out.print("Введите Id питомца: ");
        try{
            walkRequest.setPetId(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e ){
            System.out.print("Введите корректный ID питомца");
        }
        System.out.print("Введите Id хозяина: ");
        try{
            walkRequest.setOwnerId(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e ){
            System.out.print("Введите корректный ID хозяина");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.mm.yyyy HH:mm");
        System.out.print("Введите время прогулки: ");
        try{
            walkRequest.setWalkDateTime(LocalDateTime.parse(scanner.nextLine(), formatter));
        } catch (NumberFormatException e ){
            System.out.print("Введите корректное время формата dd.mm.yyyy HH:mm");
        }
        System.out.print("Введите длительность прогулки в минутах: ");
        try{
            walkRequest.setDurationMinutes(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e ){
            System.out.print("Введите корректную длительность прогулки");
        }
        System.out.print("Введите адрес прогулки: ");
        walkRequest.setWalkAddress((scanner.nextLine()));
        System.out.print("Введите описание прогулки: ");
        walkRequest.setDescription((scanner.nextLine()));
        return walkRequest;
    }

    private WalkRequest readRequestDataForUpdate(WalkRequest walkRequest){
        System.out.print("Введите Id питомца: ");
        try{
            walkRequest.setPetId(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e ){
            System.out.print("Введите корректный ID питомца");
        }
        System.out.print("Введите Id хозяина: ");
        try{
            walkRequest.setOwnerId(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e ){
            System.out.print("Введите корректный ID хозяина");
        }
        System.out.print("Введите Id выгульщика: ");
        try{
            walkRequest.setWalkerId(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e ){
            System.out.print("Введите корректный ID выгульщика");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.mm.yyyy HH:mm");
        System.out.print("Введите время прогулки: ");
        try{
            walkRequest.setWalkDateTime(LocalDateTime.parse(scanner.nextLine(), formatter));
        } catch (NumberFormatException e ){
            System.out.print("Введите корректное время формата dd.mm.yyyy HH:mm");
        }
        System.out.print("Введите длительность прогулки в минутах: ");
        try{
            walkRequest.setDurationMinutes(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e ){
            System.out.print("Введите корректную длительность прогулки");
        }
        System.out.print("Введите адрес прогулки: ");
        walkRequest.setWalkAddress((scanner.nextLine()));
        System.out.print("Введите статус прогулки: ");
        String statusText = scanner.nextLine();
        try{
            WalkStatus status = WalkStatus.valueOf(statusText.trim().toUpperCase());
            walkRequest.setStatus(status);
        } catch (IllegalArgumentException e){ 
            System.out.println("Нет такого статуса прогулки. Доступные статусы: CREATED, PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED");
        }
        
        System.out.print("Введите описание прогулки: ");
        walkRequest.setDescription((scanner.nextLine()));
        return walkRequest;
    }

    public void showMenu() {
        boolean running = true;

        while (running) {            
            System.out.println("""
                    ========= УПРАВЛЕНИЕ ЗАЯВКАМИ =========
                    1. Создать заявку
                    2. Показать все заявки
                    3. Получить заявку по ID
                    4. Обновить заявку
                    5. Удалить заявку
                    6. Поиск заявок
                    7. Назад
                    ========================================
                    """);

            System.out.print("Выберите действие: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    createRequest();
                    break;
                case "2":
                    showAllRequests();
                    break;
                case "3":   
                    findRequestById();
                    break;
                case "4":
                    updateRequest();
                break;
                case "5":
                    deleteRequest();
                    break;
                case "6":
  
                    break;
                case "7":
                    running = false;
                    break;
                default:
                    System.out.println("Такого пункта нет.");
            }

    }
    }
}
