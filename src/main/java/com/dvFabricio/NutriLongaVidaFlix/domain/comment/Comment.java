package com.dvFabricio.NutriLongaVidaFlix.domain.comment;

import com.dvFabricio.NutriLongaVidaFlix.domain.user.User;
import com.dvFabricio.NutriLongaVidaFlix.domain.video.Video;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.UUID;


@Table(name = "comments")
@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue
    private UUID id;

    private String text;

    private Date date;

    @ManyToOne
    private User user;

    @ManyToOne
    private Video video;


    public String getUserName() {
        return user != null ? user.getName() : "Usuário não disponível";
    }


    public UUID getVideoId() {
        return video != null ? video.getId() : null;
    }
}