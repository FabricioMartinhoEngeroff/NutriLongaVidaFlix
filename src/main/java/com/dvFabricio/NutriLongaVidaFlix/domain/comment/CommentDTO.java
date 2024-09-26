package com.dvFabricio.NutriLongaVidaFlix.domain.comment;

import java.util.Date;
import java.util.UUID;

public record CommentDTO(UUID id, String text, String userName, Date date, Long videoId) {

    public CommentDTO(Comment comment) {
        this(comment.getId(),
                comment.getText(),
                comment.getUserName(), // Usando o método da classe Comment
                comment.getDate(),
                comment.getVideoId()); // Usando o método da classe Comment
    }
}