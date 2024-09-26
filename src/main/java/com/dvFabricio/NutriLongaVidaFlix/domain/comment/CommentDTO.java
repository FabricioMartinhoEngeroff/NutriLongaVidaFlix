package com.dvFabricio.NutriLongaVidaFlix.domain.comment;

import java.util.Date;
import java.util.UUID;

public record CommentDTO(UUID id, String text, String userName, Date date, Long videoId) {

    public CommentDTO(Comment comment) {
        this(comment.getId(),
                comment.getText(),
                comment.getUser().getName(),
                comment.getDate(),
                comment.getVideo().getId()); // Adicionando o ID do vídeo
    }
}