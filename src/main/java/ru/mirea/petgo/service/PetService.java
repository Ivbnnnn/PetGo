
package ru.mirea.petgo.service;

import ru.mirea.petgo.exception.BusinessException;
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

    public Pet createPet(Pet pet) throws SQLException, BusinessException {
        validate(pet);
        if (userRepository.findById(pet.getOwnerId()) == null) {
            throw new BusinessException("Владелец с id=" + pet.getOwnerId() + " не найден");
        }
        return petRepository.save(pet);
    }

    public Pet updatePet(Pet pet) throws SQLException, BusinessException {
        validate(pet);
        if (petRepository.findById(pet.getId()) == null) {
            throw new BusinessException("Питомец с id=" + pet.getId() + " не найден");
        }
        if (!petRepository.update(pet)) {
            throw new BusinessException("Не удалось обновить питомца id=" + pet.getId());
        }
        return pet;
    }

    public void deletePet(int id) throws SQLException, BusinessException {
        if (petRepository.findById(id) == null) {
            throw new BusinessException("Питомец с id=" + id + " не найден");
        }
        petRepository.deleteById(id);
    }

    public Pet getById(int id) throws SQLException { return petRepository.findById(id); }
    public List<Pet> getAll() throws SQLException { return petRepository.findAll(); }
    public List<Pet> searchByName(String name) throws SQLException { return petRepository.findByName(name); }
    public List<Pet> getByOwner(int ownerId) throws SQLException { return petRepository.findByOwnerId(ownerId); }
    public List<Pet> filterByWeight(BigDecimal min, BigDecimal max) throws SQLException {
        return petRepository.findByWeightRange(min, max);
    }
    public List<Pet> filterByAge(int min, int max) throws SQLException {
        return petRepository.findByAgeRange(min, max);
    }
    public List<Pet> sorted(String sortBy, boolean asc) throws SQLException {
        return petRepository.findAllSorted(sortBy, asc);
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
    