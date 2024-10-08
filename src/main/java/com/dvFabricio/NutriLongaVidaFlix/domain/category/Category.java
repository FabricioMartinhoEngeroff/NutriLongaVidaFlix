package com.dvFabricio.NutriLongaVidaFlix.domain.category;

import com.dvFabricio.NutriLongaVidaFlix.domain.video.Video;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;

import java.util.List;

@Table(name = "categories")
@Entity
@EqualsAndHashCode(of = "id")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "category")
    private List<Video> videos;

    public Category() {}

    public Category(List<Video> videos, String name, Long id) {
        this.videos = videos;
        this.name = name;
        this.id = id;
    }

    public Category(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Video> getVideos() {
        return videos;
    }

    public void setVideos(List<Video> videos) {
        this.videos = videos;
    }
}
