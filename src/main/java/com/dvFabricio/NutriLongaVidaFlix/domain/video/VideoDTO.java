package com.dvFabricio.NutriLongaVidaFlix.domain.video;

import com.dvFabricio.NutriLongaVidaFlix.domain.comment.CommentDTO;
import com.dvFabricio.NutriLongaVidaFlix.domain.rating.RatingDTO;

import java.util.List;
import java.util.UUID;

public record VideoDTO(UUID id, String title, String description, String url, String categoryName, List<CommentDTO> comments, List<RatingDTO> ratings, double mediaAvaliacoes) {

    public VideoDTO(Video video, double mediaAvaliacoes) {
        this(video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getUrl(),
                video.getCategoryName(),
                video.getCommentDTOs(),
                video.getRatingDTOs(),
                mediaAvaliacoes);
    }
}