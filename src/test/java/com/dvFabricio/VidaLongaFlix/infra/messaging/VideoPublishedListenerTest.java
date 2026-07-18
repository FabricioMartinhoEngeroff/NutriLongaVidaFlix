package com.dvFabricio.VidaLongaFlix.infra.messaging;

import com.dvFabricio.VidaLongaFlix.domain.video.VideoPublishedMessage;
import com.dvFabricio.VidaLongaFlix.services.interaction.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class VideoPublishedListenerTest {

    @InjectMocks private VideoPublishedListener listener;
    @Mock private NotificationService notificationService;

    @Test
    void shouldCreateNotificationFromMessage() {
        UUID videoId = UUID.randomUUID();
        VideoPublishedMessage message = new VideoPublishedMessage(videoId, "Frango Grelhado");

        listener.onVideoPublished(message);

        then(notificationService).should().createForVideo(videoId, "Frango Grelhado");
    }

    @Test
    void shouldNotCallOverloadWithVideoObject() {
        UUID videoId = UUID.randomUUID();
        VideoPublishedMessage message = new VideoPublishedMessage(videoId, "Salada");

        listener.onVideoPublished(message);

        then(notificationService).should().createForVideo(videoId, "Salada");
        then(notificationService).shouldHaveNoMoreInteractions();
    }
}
