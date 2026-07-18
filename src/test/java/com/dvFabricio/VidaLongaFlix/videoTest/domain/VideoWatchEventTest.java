package com.dvFabricio.VidaLongaFlix.videoTest.domain;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoWatchEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class VideoWatchEventTest {

    private Video buildVideo() {
        return Video.builder()
                .title("Frango Grelhado")
                .description("Receita saudável")
                .url("http://example.com/video")
                .category(new Category("Receitas", CategoryType.VIDEO))
                .views(0).watchTime(0)
                .build();
    }

    @Test
    void shouldCreateEventWithAuthenticatedUser() {
        Video video = buildVideo();
        User user = new User("Fabricio", "fab@email.com", "pass", "11999999999");

        Instant before = Instant.now();
        VideoWatchEvent event = new VideoWatchEvent(video, user);
        Instant after = Instant.now();

        assertSame(video, event.getVideo());
        assertSame(user, event.getUser());
        assertNotNull(event.getWatchedAt());
        assertFalse(event.getWatchedAt().isBefore(before));
        assertFalse(event.getWatchedAt().isAfter(after));
    }

    @Test
    void shouldCreateEventWithNullUserForAnonymousView() {
        Video video = buildVideo();

        VideoWatchEvent event = new VideoWatchEvent(video, null);

        assertSame(video, event.getVideo());
        assertNull(event.getUser());
        assertNotNull(event.getWatchedAt());
    }

    @Test
    void shouldBeImmutableAfterCreation() {
        Video video = buildVideo();
        VideoWatchEvent event = new VideoWatchEvent(video, null);

        Instant capturedAt = event.getWatchedAt();

        assertEquals(capturedAt, event.getWatchedAt());
    }
}
