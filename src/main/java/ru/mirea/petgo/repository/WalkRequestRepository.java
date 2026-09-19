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
