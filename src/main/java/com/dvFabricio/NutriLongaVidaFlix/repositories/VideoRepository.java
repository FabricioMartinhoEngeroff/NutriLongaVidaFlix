package com.dvFabricio.NutriLongaVidaFlix.repositories;

import com.dvFabricio.NutriLongaVidaFlix.domain.video.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    Page<Video> findByCategory_Name(String categoryName, Pageable pageable);
    Page<Video> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<Video> findByRatings_ScoreGreaterThanEqual(int score, Pageable pageable);
}