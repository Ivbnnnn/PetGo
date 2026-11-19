package ru.mirea.petgo.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String username; // Имя/логин
    private String email;
    
    // Роли добавит Разработчик 2, здесь минимум для связей
}