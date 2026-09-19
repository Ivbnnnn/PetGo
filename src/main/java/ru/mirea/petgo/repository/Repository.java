package ru.mirea.petgo.repository;

import java.sql.SQLException;
import java.util.List;

// Общие CRUD-методы для всех репозиториев.
public interface Repository<T> {
    T save(T entity) throws SQLException;

    T findById(int id) throws SQLException;

    List<T> findAll() throws SQLException;

    boolean update(T entity) throws SQLException;

    boolean deleteById(int id) throws SQLException;
}
