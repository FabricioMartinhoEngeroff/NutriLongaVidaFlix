package com.dvFabricio.VidaLongaFlix.infra.messaging;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.services.interaction.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SyncVideoEventPublisherTest {

    @InjectMocks private SyncVideoEventPublisher publisher;
    @Mock private NotificationService notificationService;

    @Test
    void shouldCallNotificationServiceDirectly() {
        Video video = Video.builder()
                .title("Frango").description("Desc")
                .url("http://url.com")
                .category(new Category("Cat", CategoryType.VIDEO))
                .views(0).watchTime(0).build();

        publisher.publishVideoPublished(video);

        then(notificationService).should().createForVideo(video);
    }
}
