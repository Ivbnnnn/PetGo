package ru.mirea.petgo.repository;

import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.model.enums.WalkStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WalkRequestRepository implements Repository<WalkRequest> {
    private final Connection connection;

    public WalkRequestRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public WalkRequest save(WalkRequest request) throws SQLException {
        String sql = "INSERT INTO walk_requests (pet_id, owner_id, walker_id, walk_datetime, "
                + "duration_minutes, walk_address, status, description, price) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setRequestValues(statement, request);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    request.setId(keys.getInt(1));
                }
            }
        }
        return request;
    }

    @Override
    public WalkRequest findById(int id) throws SQLException {
        String sql = "SELECT * FROM walk_requests WHERE id = ?";

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
    public List<WalkRequest> findAll() throws SQLException {
        List<WalkRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM walk_requests ORDER BY id";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                requests.add(mapRow(resultSet));
            }
        }
        return requests;
    }

    @Override
    public boolean update(WalkRequest request) throws SQLException {
        String sql = "UPDATE walk_requests SET pet_id = ?, owner_id = ?, walker_id = ?, "
                + "walk_datetime = ?, duration_minutes = ?, walk_address = ?, status = ?, "
                + "description = ?, price = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setRequestValues(statement, request);
            statement.setInt(10, request.getId());
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteById(int id) throws SQLException {
        String sql = "DELETE FROM walk_requests WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }


    public List<WalkRequest> findByPetName(String petName) throws SQLException {
        String sql = "SELECT wr.* FROM walk_requests wr "
                + "JOIN pets p ON wr.pet_id = p.id "
                + "WHERE p.name ILIKE ? ORDER BY p.name";
        List<WalkRequest> requests = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + petName + "%");
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) requests.add(mapRow(rs));
            }
        }
        return requests;
    }

    public List<WalkRequest> findByDate(LocalDate date) throws SQLException {
        String sql = "SELECT * FROM walk_requests "
                + "WHERE DATE(walk_datetime) = ? ORDER BY walk_datetime";
        List<WalkRequest> requests = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) requests.add(mapRow(rs));
            }
        }
        return requests;
    }


    public List<WalkRequest> findByStatus(WalkStatus status) throws SQLException {
        String sql = "SELECT * FROM walk_requests WHERE status = ? ORDER BY walk_datetime";
        List<WalkRequest> requests = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) requests.add(mapRow(rs));
            }
        }
        return requests;
    }

    public List<WalkRequest> findByOwnerId(int ownerId) throws SQLException {
        String sql = "SELECT * FROM walk_requests WHERE owner_id = ? ORDER BY walk_datetime";
        List<WalkRequest> requests = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ownerId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) requests.add(mapRow(rs));
            }
        }
        return requests;
    }


    public List<WalkRequest> findAllSorted(String field, boolean asc) throws SQLException {
        String column;
        switch (field) {
            case "walk_datetime":
            case "created_at":
            case "status":
            case "price":
            case "duration_minutes":
                column = field;
                break;
            default:
                throw new IllegalArgumentException("Недопустимое поле сортировки: " + field);
        }
        String direction = asc ? "ASC" : "DESC";
        String sql = "SELECT * FROM walk_requests ORDER BY " + column + " " + direction;

        List<WalkRequest> requests = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) requests.add(mapRow(rs));
        }
        return requests;
    }

    public List<WalkRequest> findAllSortedByPetName(boolean asc) throws SQLException {
        String direction = asc ? "ASC" : "DESC";
        String sql = "SELECT wr.* FROM walk_requests wr "
                + "JOIN pets p ON wr.pet_id = p.id "
                + "ORDER BY p.name " + direction;

        List<WalkRequest> requests = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) requests.add(mapRow(rs));
        }
        return requests;
    }


    private void setRequestValues(PreparedStatement statement, WalkRequest request) throws SQLException {
        statement.setInt(1, request.getPetId());
        statement.setInt(2, request.getOwnerId());
        if (request.getWalkerId() == null) {
            statement.setNull(3, Types.INTEGER);
        } else {
            statement.setInt(3, request.getWalkerId());
        }
        statement.setTimestamp(4, Timestamp.valueOf(request.getWalkDateTime()));
        statement.setInt(5, request.getDurationMinutes());
        statement.setString(6, request.getWalkAddress());
        statement.setString(7, request.getStatus().name());
        statement.setString(8, request.getDescription());
        statement.setBigDecimal(9, request.getPrice());
    }

    private WalkRequest mapRow(ResultSet resultSet) throws SQLException {
        WalkRequest request = new WalkRequest();
        request.setId(resultSet.getInt("id"));
        request.setPetId(resultSet.getInt("pet_id"));
        request.setOwnerId(resultSet.getInt("owner_id"));
        request.setWalkerId((Integer) resultSet.getObject("walker_id"));

        Timestamp walkDateTime = resultSet.getTimestamp("walk_datetime");
        if (walkDateTime != null) {
            request.setWalkDateTime(walkDateTime.toLocalDateTime());
        }
        request.setDurationMinutes(resultSet.getInt("duration_minutes"));
        request.setWalkAddress(resultSet.getString("walk_address"));
        request.setStatus(WalkStatus.valueOf(resultSet.getString("status")));
        request.setDescription(resultSet.getString("description"));
        request.setPrice(resultSet.getBigDecimal("price"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            request.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            request.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return request;
    }
}