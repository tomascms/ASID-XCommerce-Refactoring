package com.xcommerce.catalog_service.dto;

public class ReviewResponse {
    private Long id;
    private String comment;
    private Float rating;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Float getRating() { return rating; }
    public void setRating(Float rating) { this.rating = rating; }

    public static ReviewResponse from(com.xcommerce.catalog_service.model.Review review) {
        ReviewResponse dto = new ReviewResponse();
        dto.setId(review.getId());
        dto.setComment(review.getEval());
        dto.setRating(review.getGrade());
        return dto;
    }
}
