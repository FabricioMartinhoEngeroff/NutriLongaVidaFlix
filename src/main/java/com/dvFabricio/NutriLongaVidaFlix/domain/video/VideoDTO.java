package com.dvFabricio.NutriLongaVidaFlix.domain.video;

import com.dvFabricio.NutriLongaVidaFlix.domain.category.Category;
import com.dvFabricio.NutriLongaVidaFlix.domain.comment.CommentDTO;
import com.dvFabricio.NutriLongaVidaFlix.domain.rating.RatingDTO;

import java.util.List;

public record VideoDTO(Long id, String title, String description, String url, String categoryName, List<CommentDTO> comments, List<RatingDTO> ratings, double mediaAvaliacoes) {

    public VideoDTO(Video video, double mediaAvaliacoes) {
        this(video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getUrl(),
                video.getCategory() != null ? video.getCategory().getName() : "Categoria não disponível",
                video.getComments().stream().map(CommentDTO::new).toList(),
                video.getRatings().stream().map(RatingDTO::new).toList(),
                mediaAvaliacoes);
    }
}