package ru.mirea.petgo.model;

import java.time.LocalDateTime;

public class WalkHistory {
    private int id;
    private int walkRequestId;
    private Integer actualDuration;
    private String route;
    private String ownerReview;
    private String walkerReview;
    private Integer rating;
    private LocalDateTime completedAt;

    public WalkHistory() {
    }

    public WalkHistory(int id, int walkRequestId, Integer actualDuration, String route,
                       String ownerReview, String walkerReview, Integer rating,
                       LocalDateTime completedAt) {
        this.id = id;
        this.walkRequestId = walkRequestId;
        this.actualDuration = actualDuration;
        this.route = route;
        this.ownerReview = ownerReview;
        this.walkerReview = walkerReview;
        this.rating = rating;
        this.completedAt = completedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getWalkRequestId() { return walkRequestId; }
    public void setWalkRequestId(int walkRequestId) { this.walkRequestId = walkRequestId; }
    public Integer getActualDuration() { return actualDuration; }
    public void setActualDuration(Integer actualDuration) { this.actualDuration = actualDuration; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public String getOwnerReview() { return ownerReview; }
    public void setOwnerReview(String ownerReview) { this.ownerReview = ownerReview; }
    public String getWalkerReview() { return walkerReview; }
    public void setWalkerReview(String walkerReview) { this.walkerReview = walkerReview; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
