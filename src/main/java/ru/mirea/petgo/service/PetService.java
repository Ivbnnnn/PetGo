
package ru.mirea.petgo.service;

import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.model.Pet;
import ru.mirea.petgo.repository.PetRepository;
import ru.mirea.petgo.repository.UserRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

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
        if (pet.getName() == null || pet.getName().isBlank())
            throw new BusinessException("Кличка не может быть пустой");
        if (pet.getOwnerId() <= 0)
            throw new BusinessException("Некорректный id владельца");
        if (pet.getWeight() != null && pet.getWeight().compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException("Вес должен быть положительным");
        if (pet.getAge() != null && pet.getAge() < 0)
            throw new BusinessException("Возраст не может быть отрицательным");
    }
}
    
