    package ru.mirea.petgo;

    import java.sql.Connection;
    import java.sql.DriverManager;
    import java.sql.ResultSet;
    import java.sql.Statement;

    public class Main {

        public static void main(String[] args) {

            String url = "jdbc:postgresql://127.0.0.1:5434/pet_db";
            String user = "postgres";
            String password = "postgres";

            try (
                Connection connection = DriverManager.getConnection(
                    url,
                    user,
                    password
                )
            ) {

                System.out.println("Подключение к PostgreSQL успешно");

                Statement statement = connection.createStatement();

                statement.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL
                    )
                """);

                statement.executeUpdate("""
                    INSERT INTO users(name)
                    VALUES ('Ivan')
                """);

                ResultSet result = statement.executeQuery("""
                    SELECT id, name
                    FROM users
                """);

                while (result.next()) {
                    int id = result.getInt("id");
                    String name = result.getString("name");

                    System.out.println(id + " | " + name);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
