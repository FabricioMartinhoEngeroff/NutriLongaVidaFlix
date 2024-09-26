package com.dvFabricio.NutriLongaVidaFlix.repositories;

import com.dvFabricio.NutriLongaVidaFlix.domain.rating.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByVideo_Id(Long videoId);

}
