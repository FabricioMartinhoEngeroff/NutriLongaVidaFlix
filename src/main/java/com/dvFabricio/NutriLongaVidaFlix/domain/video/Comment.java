package com.dvFabricio.NutriLongaVidaFlix.domain.video;

import com.dvFabricio.NutriLongaVidaFlix.domain.user.User;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Table(name = "comments")
@Entity(name = "comments")
@EqualsAndHashCode(of = "id")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private User user;
    private String text;
    private Date date;
    @ManyToOne
    private Video video;

    public Comment() {}

    public Comment(User user, String text, Date date, Video video) {
        this.user = user;
        this.text = text;
        this.date = date;
        this.video = video;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Video getVideo() {
        return video;
    }

    public void setVideo(Video video) {
        this.video = video;
    }
}
