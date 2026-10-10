
package ru.mirea.petgo.service;

import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.ValidationException;
import ru.mirea.petgo.model.Pet;
import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.UserRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import ru.mirea.petgo.util.Validators;

public class PetService {
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    public PetService(PetRepository petRepository, UserRepository userRepository) {
        this.petRepository = petRepository;
        this.userRepository = userRepository;
    }

    public Pet createPet(Pet pet) throws BusinessException, DatabaseException {
        validate(pet);
        try {
            if (userRepository.findById(pet.getOwnerId()) == null) {
                throw new BusinessException("Владелец с id=" + pet.getOwnerId() + " не найден");
            }
            return petRepository.save(pet);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка сохранения питомца", e);
        }
    }

    public Pet updatePet(Pet pet) throws BusinessException, DatabaseException {
        validate(pet);
        try {
            if (petRepository.findById(pet.getId()) == null) {
                throw new BusinessException("Питомец с id=" + pet.getId() + " не найден");
            }
            if (!petRepository.update(pet)) {
                throw new BusinessException("Не удалось обновить питомца id=" + pet.getId());
            }
            return pet;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка обновления питомца id=" + pet.getId(), e);
        }
    }

    public void deletePet(int id) throws BusinessException, DatabaseException {
        try {
            if (petRepository.findById(id) == null) {
                throw new BusinessException("Питомец с id=" + id + " не найден");
            }
            petRepository.deleteById(id);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка удаления питомца id=" + id, e);
        }
    }

    public Pet getById(int id) throws DatabaseException {
        try {
            return petRepository.findById(id);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска питомца по id=" + id, e);
        }
    }

    public List<Pet> getAll() throws DatabaseException {
        try {
            return petRepository.findAll();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка получения списка питомцев", e);
        }
    }

    public List<Pet> searchByName(String name) throws DatabaseException {
        try {
            return petRepository.findByName(name);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска питомцев по имени", e);
        }
    }

    public List<Pet> getByOwner(int ownerId) throws DatabaseException {
        try {
            return petRepository.findByOwnerId(ownerId);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска питомцев владельца id=" + ownerId, e);
        }
    }

    public List<Pet> filterByWeight(BigDecimal min, BigDecimal max) throws DatabaseException {
        try {
            return petRepository.findByWeightRange(min, max);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка фильтрации питомцев по весу", e);
        }
    }

    public List<Pet> filterByAge(int min, int max) throws DatabaseException {
        try {
            return petRepository.findByAgeRange(min, max);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка фильтрации питомцев по возрасту", e);
        }
    }

    public List<Pet> sorted(String sortBy, boolean asc) throws DatabaseException {
        try {
            return petRepository.findAllSorted(sortBy, asc);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка сортировки питомцев", e);
        }
    }

    private void validate(Pet pet) throws BusinessException {
        Validators.requireText("name", "Кличка", pet.getName(), 100);
        Validators.optionalText("breed", "Порода", pet.getBreed(), 100);
        Validators.optionalText("photoUrl", "Фото", pet.getPhotoUrl(), 255);
        if (pet.getAge() < 0 && pet.getAge() != null) {
            throw new ValidationException("age", "Возраст питомца не может быть отрицательным");
        }
        if (pet.getWeight() != null) {
            Validators.range("weight", "Вес", pet.getWeight(),
                    new BigDecimal("0.01"), new BigDecimal("999.99"));
        }
    }
}
