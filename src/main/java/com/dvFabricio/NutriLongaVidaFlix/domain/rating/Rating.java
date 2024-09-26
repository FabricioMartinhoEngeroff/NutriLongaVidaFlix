package com.dvFabricio.NutriLongaVidaFlix.domain.rating;

import com.dvFabricio.NutriLongaVidaFlix.domain.user.User;
import com.dvFabricio.NutriLongaVidaFlix.domain.video.Video;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;

@Table(name = "ratings")
@Entity
@EqualsAndHashCode(of = "id")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int rating;

    @ManyToOne
    private User user;

    @ManyToOne
    private Video video;

    public Rating() {}

    public Rating(User user, int rating, Video video) {
        this.user = user;
        this.rating = rating;
        this.video = video;
    }

    public Long getId() {
        return id;
    }

    public int getRating() {
        return rating;
    }

    public User getUser() {
        return user;
    }

    public Video getVideo() {
        return video;
    }

    public int getScore() {
        return rating;
    }
}