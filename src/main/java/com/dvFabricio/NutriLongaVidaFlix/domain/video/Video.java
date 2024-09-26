package com.dvFabricio.NutriLongaVidaFlix.domain.video;

import com.dvFabricio.NutriLongaVidaFlix.domain.category.Category;
import com.dvFabricio.NutriLongaVidaFlix.domain.comment.Comment;
import com.dvFabricio.NutriLongaVidaFlix.domain.comment.CommentDTO;
import com.dvFabricio.NutriLongaVidaFlix.domain.rating.Rating;
import com.dvFabricio.NutriLongaVidaFlix.domain.rating.RatingDTO;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Table(name = "videos")
@Entity
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id
    @GeneratedValue
    private UUID id;

    private String title;
    private String description;
    private String url;

    @ManyToOne(optional = true)
    private Category category;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> comments;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Rating> ratings;

    public String getCategoryName() {
        return category != null ? category.getName() : "Categoria não disponível";
    }

    public List<CommentDTO> getCommentDTOs() {
        return comments.stream().map(CommentDTO::new).collect(Collectors.toList());
    }

    public List<RatingDTO> getRatingDTOs() {
        return ratings.stream().map(RatingDTO::new).collect(Collectors.toList());
    }
}
