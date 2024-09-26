package com.dvFabricio.NutriLongaVidaFlix.repositories;

import com.dvFabricio.NutriLongaVidaFlix.domain.comment.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByVideo_Id(Long videoId, Pageable pageable);
}