package com.dvFabricio.NutriLongaVidaFlix.domain.rating;

public record RatingDTO(Long id, int score, String userName) {

    public RatingDTO(Rating rating) {
        this(rating.getId(), rating.getRating(), rating.getUser().getLogin());
    }
}
