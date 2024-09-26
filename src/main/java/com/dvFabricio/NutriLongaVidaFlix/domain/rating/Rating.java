package com.dvFabricio.NutriLongaVidaFlix.domain.rating;

import com.dvFabricio.NutriLongaVidaFlix.domain.user.User;
import com.dvFabricio.NutriLongaVidaFlix.domain.video.Video;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "ratings")
@EqualsAndHashCode(of = "id")
@Getter
@Setter
@NoArgsConstructor
public class Rating {

    @Id
    @GeneratedValue
    private UUID id;

    private int rating;

    @ManyToOne
    private User user;

    @ManyToOne
    private Video video;

    public Rating(User user, int rating, Video video) {
        this.user = user;
        this.rating = rating;
        this.video = video;
    }
}