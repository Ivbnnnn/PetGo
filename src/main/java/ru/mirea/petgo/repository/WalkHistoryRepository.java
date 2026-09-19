package ru.mirea.petgo.repository;

import ru.mirea.petgo.model.WalkHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class WalkHistoryRepository implements Repository<WalkHistory> {
    private final Connection connection;

    public WalkHistoryRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public WalkHistory save(WalkHistory history) throws SQLException {
        String sql = "INSERT INTO walk_history (walk_request_id, actual_duration, route, "
                + "owner_review, walker_review, rating) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setHistoryValues(statement, history);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    history.setId(keys.getInt(1));
                }
            }
        }
        return history;
    }

    @Override
    public WalkHistory findById(int id) throws SQLException {
        String sql = "SELECT * FROM walk_history WHERE id = ?";

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
    public List<WalkHistory> findAll() throws SQLException {
        List<WalkHistory> histories = new ArrayList<>();
        String sql = "SELECT * FROM walk_history ORDER BY id";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                histories.add(mapRow(resultSet));
            }
        }
        return histories;
    }

    @Override
    public boolean update(WalkHistory history) throws SQLException {
        String sql = "UPDATE walk_history SET walk_request_id = ?, actual_duration = ?, route = ?, "
                + "owner_review = ?, walker_review = ?, rating = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setHistoryValues(statement, history);
            statement.setInt(7, history.getId());
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteById(int id) throws SQLException {
        String sql = "DELETE FROM walk_history WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private void setHistoryValues(PreparedStatement statement, WalkHistory history) throws SQLException {
        statement.setInt(1, history.getWalkRequestId());
        statement.setObject(2, history.getActualDuration());
        statement.setString(3, history.getRoute());
        statement.setString(4, history.getOwnerReview());
        statement.setString(5, history.getWalkerReview());
        statement.setObject(6, history.getRating());
    }

    private WalkHistory mapRow(ResultSet resultSet) throws SQLException {
        WalkHistory history = new WalkHistory();
        history.setId(resultSet.getInt("id"));
        history.setWalkRequestId(resultSet.getInt("walk_request_id"));
        history.setActualDuration((Integer) resultSet.getObject("actual_duration"));
        history.setRoute(resultSet.getString("route"));
        history.setOwnerReview(resultSet.getString("owner_review"));
        history.setWalkerReview(resultSet.getString("walker_review"));
        history.setRating((Integer) resultSet.getObject("rating"));

        Timestamp completedAt = resultSet.getTimestamp("completed_at");
        if (completedAt != null) {
            history.setCompletedAt(completedAt.toLocalDateTime());
        }
        return history;
    }
}
