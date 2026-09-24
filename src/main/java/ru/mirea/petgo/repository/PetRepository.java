package ru.mirea.petgo.repository;

import ru.mirea.petgo.model.Pet;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PetRepository implements Repository<Pet> {
    private final Connection connection;

    public PetRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Pet save(Pet pet) throws SQLException {
        String sql = "INSERT INTO pets (name, breed, weight, age, special_needs, owner_id, photo_url) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, pet.getName());
            statement.setString(2, pet.getBreed());
            statement.setBigDecimal(3, pet.getWeight());
            statement.setObject(4, pet.getAge());
            statement.setString(5, pet.getSpecialNeeds());
            statement.setInt(6, pet.getOwnerId());
            statement.setString(7, pet.getPhotoUrl());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    pet.setId(keys.getInt(1));
                }
            }
        }
        return pet;
    }

    @Override
    public Pet findById(int id) throws SQLException {
        String sql = "SELECT * FROM pets WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public List<Pet> findAll() throws SQLException {
        List<Pet> pets = new ArrayList<>();
        String sql = "SELECT * FROM pets ORDER BY id";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                pets.add(mapRow(resultSet));
            }
        }
        return pets;
    }

    @Override
    public boolean update(Pet pet) throws SQLException {
        String sql = "UPDATE pets SET name = ?, breed = ?, weight = ?, age = ?, special_needs = ?, "
                + "owner_id = ?, photo_url = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pet.getName());
            statement.setString(2, pet.getBreed());
            statement.setBigDecimal(3, pet.getWeight());
            statement.setObject(4, pet.getAge());
            statement.setString(5, pet.getSpecialNeeds());
            statement.setInt(6, pet.getOwnerId());
            statement.setString(7, pet.getPhotoUrl());
            statement.setInt(8, pet.getId());
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteById(int id) throws SQLException {
        String sql = "DELETE FROM pets WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private Pet mapRow(ResultSet resultSet) throws SQLException {
        Pet pet = new Pet();
        pet.setId(resultSet.getInt("id"));
        pet.setName(resultSet.getString("name"));
        pet.setBreed(resultSet.getString("breed"));
        pet.setWeight(resultSet.getBigDecimal("weight"));
        pet.setAge((Integer) resultSet.getObject("age"));
        pet.setSpecialNeeds(resultSet.getString("special_needs"));
        pet.setOwnerId(resultSet.getInt("owner_id"));
        pet.setPhotoUrl(resultSet.getString("photo_url"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            pet.setCreatedAt(createdAt.toLocalDateTime());
        }
        return pet;
    }


    public List<Pet> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM pets WHERE name ILIKE ? ORDER BY name";
        List<Pet> pets = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setString(1,"%" + name + "%" );
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) pets.add(mapRow(rs));
            }
        }
        return pets;
    }

    public List<Pet> findByOwnerId(int ownerId) throws SQLException {
        String sql = "SELECT * FROM pets WHERE owner_id = ? ORDER BY name";
        List<Pet> pets = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ownerId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) pets.add(mapRow(rs));
            }
        }
        return pets;
    }

    public List<Pet> findByBreed(String breed) throws SQLException {
        String sql = "SELECT * FROM pets WHERE breed ILIKE ? ORDER BY breed";
        List<Pet> pets = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + breed + "%");
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) pets.add(mapRow(rs));
            }        
        }
        return pets;
    }

    public List<Pet> findByWeightRange(BigDecimal min, BigDecimal max) throws SQLException {
        String sql = "SELECT * FROM pets WHERE weight BETWEEN ? AND ? ORDER BY weight";
        List<Pet> pets = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, min);
            statement.setBigDecimal(2, max);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) pets.add(mapRow(rs));
            }
        }
        return pets;
    }

    public List<Pet> findByAgeRange(int minAge, int maxAge) throws SQLException {
        String sql = "SELECT * FROM pets WHERE age BETWEEN ? AND ? ORDER BY age";
        List<Pet> pets = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, minAge);
            statement.setInt(2, maxAge);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) pets.add(mapRow(rs));
            }
        }
        return pets;
    }

    public List<Pet> findByOwnerAndBreed(int ownerId, String breed) throws SQLException {
        String sql = "SELECT * FROM pets WHERE owner_id = ? AND breed ILIKE ? ORDER BY name";
        List<Pet> pets = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ownerId);
            statement.setString(2, "%" + breed + "%");
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) pets.add(mapRow(rs));
            }
        }   
        return pets;
    }

    public List<Pet> findAllSorted(String sortBy, boolean ascending) throws SQLException {
        Comparator<Pet> comparator = switch (sortBy) {
            case "name"   -> Comparator.comparing(Pet::getName, String.CASE_INSENSITIVE_ORDER);
            case "breed"  -> Comparator.comparing(Pet::getBreed, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "weight" -> Comparator.comparing(Pet::getWeight, Comparator.nullsLast(BigDecimal::compareTo));
            case "age"    -> Comparator.comparing(Pet::getAge, Comparator.nullsLast(Integer::compareTo));
            case "id"     -> Comparator.comparingInt(Pet::getId);
            default -> throw new IllegalArgumentException("Недопустимое поле сортировки: " + sortBy);
        };
        if (!ascending) comparator = comparator.reversed();

        return findAll().stream()
                .sorted(comparator)
                .toList();
    }
}
