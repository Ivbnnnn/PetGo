package ru.mirea.petgo.model;

import ru.mirea.petgo.model.enums.WalkStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalkRequest {
    private int id;
    private int petId;
    private int ownerId;
    private Integer walkerId;
    private LocalDateTime walkDateTime;
    private int durationMinutes;
    private String walkAddress;
    private WalkStatus status;
    private String description;
    private BigDecimal price;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WalkRequest() {
    }

    public WalkRequest(int id, int petId, int ownerId, Integer walkerId,
                       LocalDateTime walkDateTime, int durationMinutes, String walkAddress,
                       WalkStatus status, String description, BigDecimal price,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.petId = petId;
        this.ownerId = ownerId;
        this.walkerId = walkerId;
        this.walkDateTime = walkDateTime;
        this.durationMinutes = durationMinutes;
        this.walkAddress = walkAddress;
        this.status = status;
        this.description = description;
        this.price = price;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPetId() { return petId; }
    public void setPetId(int petId) { this.petId = petId; }
    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public Integer getWalkerId() { return walkerId; }
    public void setWalkerId(Integer walkerId) { this.walkerId = walkerId; }
    public LocalDateTime getWalkDateTime() { return walkDateTime; }
    public void setWalkDateTime(LocalDateTime walkDateTime) { this.walkDateTime = walkDateTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getWalkAddress() { return walkAddress; }
    public void setWalkAddress(String walkAddress) { this.walkAddress = walkAddress; }
    public WalkStatus getStatus() { return status; }
    public void setStatus(WalkStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
