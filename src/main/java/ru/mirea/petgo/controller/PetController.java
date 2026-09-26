package ru.mirea.petgo.controller;

import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.EntityNotFoundException;
import ru.mirea.petgo.model.Pet;
import ru.mirea.petgo.service.PetService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class PetController {
    private final Scanner scanner;
    private final PetService petService;

    public PetController(Scanner scanner, PetService petService) {
        this.scanner = scanner;
        this.petService = petService;
    }

    private void createPetMenu() {
        try {
            Pet pet = readPetData(new Pet());
            petService.createPet(pet);
            System.out.println("Питомец добавлен. ID: " + pet.getId());

        } catch (NumberFormatException e) {
            System.out.println("Вес, возраст и ID владельца должны быть числами.");
        } catch (BusinessException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println("Ошибка базы данных: " + e.getMessage());
        }
    }

    private void showAllPets() {
        try {
            List<Pet> pets = petService.getAll();

            if (pets.isEmpty()) {
                System.out.println("Питомцев нет.");
                return;
            }

            System.out.println("Список питомцев:");
            for (Pet pet : pets) {
                printPet(pet);
            }

        } catch (DatabaseException e) {
            System.out.println("Ошибка базы данных: " + e.getMessage());
        }
    }

    private void getPetById() {
        try {
            System.out.print("Введите ID питомца: ");
            int petId = Integer.parseInt(scanner.nextLine());

            Pet pet = petService.getById(petId);
            if (pet == null) {
                System.out.println("Питомец не найден.");
                return;
            }

            System.out.println("Питомец найден:");
            printPet(pet);

        } catch (NumberFormatException e) {
            System.out.println("ID должен быть числом.");
        } catch (DatabaseException e) {
            System.out.println("Ошибка базы данных: " + e.getMessage());
        }
    }

    private void updatePet() {
        try {
            System.out.print("Введите ID питомца для обновления: ");
            int petId = Integer.parseInt(scanner.nextLine());

            Pet pet = petService.getById(petId);
            if (pet == null) {
                System.out.println("Питомец не найден.");
                return;
            }

            System.out.println("Введите новые данные питомца:");
            readPetData(pet);
            petService.updatePet(pet);
            System.out.println("Питомец обновлён.");

        } catch (NumberFormatException e) {
            System.out.println("Вес, возраст и ID владельца должны быть числами.");
        } catch (BusinessException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println("Ошибка базы данных: " + e.getMessage());
        }
    }

    private void deletePet() {
        try {
            System.out.print("Введите ID питомца для удаления: ");
            int petId = Integer.parseInt(scanner.nextLine());

            petService.deletePet(petId);
            System.out.println("Питомец удалён.");

        } catch (NumberFormatException e) {
            System.out.println("ID должен быть числом.");
        } catch (BusinessException e) {
            System.out.println(e.getMessage());
        } catch (DatabaseException e) {
            System.out.println("Ошибка базы данных: " + e.getMessage());
        }
    }

    private Pet readPetData(Pet pet) {
        System.out.print("Введите имя питомца: ");
        pet.setName(scanner.nextLine());

        System.out.print("Введите породу: ");
        pet.setBreed(scanner.nextLine());

        System.out.print("Введите вес: ");
        pet.setWeight(new BigDecimal(scanner.nextLine()));

        System.out.print("Введите возраст: ");
        pet.setAge(Integer.parseInt(scanner.nextLine()));

        System.out.print("Введите особые потребности: ");
        pet.setSpecialNeeds(scanner.nextLine());

        System.out.print("Введите ID владельца: ");
        pet.setOwnerId(Integer.parseInt(scanner.nextLine()));

        System.out.print("Введите ссылку на фото: ");
        pet.setPhotoUrl(scanner.nextLine());

        return pet;
    }

    private void printPet(Pet pet) {
        System.out.println(
                "ID: " + pet.getId()
                        + ", имя: " + pet.getName()
                        + ", порода: " + pet.getBreed()
                        + ", вес: " + pet.getWeight()
                        + ", возраст: " + pet.getAge()
                        + ", владелец ID: " + pet.getOwnerId()
        );
    }

    private void findPet(){
        try{
            System.out.println("""
                1. Найти по ID хозяина
                2. Найти по имени
                3. Найти по весу
                4. Выйти
                """);
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":{
                    System.out.print("Ввидет ID хозяина: ");
                    Integer ownerId = Integer.parseInt(scanner.nextLine());
                    List<Pet> pets= petService.getByOwner(ownerId);
                    if (pets.isEmpty()){
                        System.out.println("У пользователя с ID" + ownerId + "нет питомцев");
                    }
                    for (Pet pet : pets){
                        printPet(pet);
                    }
                    break;}
                case "2":{
                    System.out.print("Ввидет имя питомца: ");
                    String name = scanner.nextLine();
                    List<Pet> pets= petService.searchByName(name);
                    if (pets.isEmpty()){
                        System.out.println("Нет питомцев с именем" + pets);
                    }
                    for (Pet pet : pets){
                        printPet(pet);
                    }
                    break;}
                case "3":{
                    System.out.print("Минимальный вес: ");
                    BigDecimal minWeight = new BigDecimal(scanner.nextLine());
                    System.out.print("Максимальный вес: ");
                    BigDecimal maxWeight = new BigDecimal(scanner.nextLine());
                    List<Pet> pets = petService.filterByWeight(minWeight, maxWeight);
                    if (pets.isEmpty()){
                        System.out.println("Нет питомцев с именем" + pets);
                    }
                    for (Pet pet : pets){
                        printPet(pet);
                    }
                    break;}
                case "4":
                    return;
            
                default:
                    System.out.println("Нет такого варианта");
            }
        } catch (DatabaseException e){
            System.out.println(e.getMessage());
        } catch (NumberFormatException e){
            System.out.println("Введите корректные данные:" + e.getMessage());
        }
    }

    public void showMenu() {
        boolean running = true;

        while (running) {


            
            System.out.println("""
                    ========= УПРАВЛЕНИЕ ПИТОМЦАМИ =========
                    1. Добавить питомца
                    2. Показать всех питомцев
                    3. Получить питомца по ID
                    4. Обновить питомца
                    5. Удалить питомца
                    6. Поиск питомцев
                    7. Назад
                    ========================================
                    """);

            System.out.print("Выберите действие: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    createPetMenu();
                    break;
                case "2":
                    showAllPets();
                    break;
                case "3":
                    getPetById();
                    break;
                case "4":
                    updatePet();
                    break;
                case "5":
                    deletePet();
                    break;
                case "6":
                    findPet();
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
