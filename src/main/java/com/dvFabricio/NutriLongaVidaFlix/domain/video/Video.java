package com.dvFabricio.NutriLongaVidaFlix.domain.video;

import com.dvFabricio.NutriLongaVidaFlix.domain.category.Category;
import com.dvFabricio.NutriLongaVidaFlix.domain.comment.Comment;
import com.dvFabricio.NutriLongaVidaFlix.domain.rating.Rating;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;

import java.util.List;

@Table(name = "videos")
@Entity
@EqualsAndHashCode(of = "id")
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private String url;

    @ManyToOne
    private Category category;

    @OneToMany(mappedBy = "video")
    private List<Comment> comments;

    @OneToMany(mappedBy = "video")
    private List<Rating> ratings;

    public Video() {
    }

    public Video(String title, String description, Category category) {
        this.title = title;
        this.description = description;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<Rating> getRatings() {
        return ratings;
    }

    public void setRatings(List<Rating> ratings) {
        this.ratings = ratings;
    }
}
