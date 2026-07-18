package com.dvFabricio.VidaLongaFlix.repositories;

import com.dvFabricio.VidaLongaFlix.domain.video.VideoWatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface VideoWatchEventRepository extends JpaRepository<VideoWatchEvent, UUID> {

    long countByVideo_Id(UUID videoId);

    @Query("SELECT COUNT(DISTINCT e.user) FROM VideoWatchEvent e WHERE e.video.id = :videoId AND e.user IS NOT NULL")
    long countDistinctUsersByVideoId(@Param("videoId") UUID videoId);
}
