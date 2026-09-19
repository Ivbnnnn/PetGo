package ru.mirea.petgo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Pet {
    private int id;
    private String name;
    private String breed;
    private BigDecimal weight;
    private Integer age;
    private String specialNeeds;
    private int ownerId;
    private String photoUrl;
    private LocalDateTime createdAt;

    public Pet() {
    }

    public Pet(int id, String name, String breed, BigDecimal weight, Integer age,
               String specialNeeds, int ownerId, String photoUrl, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.breed = breed;
        this.weight = weight;
        this.age = age;
        this.specialNeeds = specialNeeds;
        this.ownerId = ownerId;
        this.photoUrl = photoUrl;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getSpecialNeeds() { return specialNeeds; }
    public void setSpecialNeeds(String specialNeeds) { this.specialNeeds = specialNeeds; }
    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
