package com.dvFabricio.VidaLongaFlix.domain.video;

import com.dvFabricio.VidaLongaFlix.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "video_watch_events")
@Getter
@NoArgsConstructor
public class VideoWatchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "watched_at", nullable = false)
    private Instant watchedAt;

    public VideoWatchEvent(Video video, User user) {
        this.video = video;
        this.user = user;
        this.watchedAt = Instant.now();
    }
}
