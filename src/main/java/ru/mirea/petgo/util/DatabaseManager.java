package ru.mirea.petgo.util;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://localhost:5434/pet_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";

    static{
        try{
            Class.forName("org.postgresql.Driver");
        }
        catch(ClassNotFoundException e){
            throw new RuntimeException("PostgreSQL JDBC driver not found", e);
        }
    }
    private DatabaseManager(){}
    public static Connection getConnection() throws SQLException{
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
