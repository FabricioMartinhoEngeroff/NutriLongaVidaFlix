package com.dvFabricio.NutriLongaVidaFlix.domain.comment;

import java.util.Date;

public record CommentDTO(Long id, String text, String userName, Date date) {

    public CommentDTO(Comment comment) {
        this(comment.getId(), comment.getText(), comment.getUser().getName(), comment.getDate());
    }

}