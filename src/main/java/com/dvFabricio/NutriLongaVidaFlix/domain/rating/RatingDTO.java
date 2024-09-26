package com.dvFabricio.NutriLongaVidaFlix.domain.rating;

import java.util.UUID;

public record RatingDTO(UUID id, int score, String userName) {

    public RatingDTO(Rating rating) {
        this(rating.getId(), rating.getRating(), rating.getUser().getLogin());
    }
}
