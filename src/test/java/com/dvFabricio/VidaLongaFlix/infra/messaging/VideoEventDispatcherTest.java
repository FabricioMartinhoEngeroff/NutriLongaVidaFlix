package com.dvFabricio.VidaLongaFlix.infra.messaging;

import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class VideoEventDispatcherTest {

    @InjectMocks private VideoEventDispatcher dispatcher;
    @Mock private VideoEventPublisher videoEventPublisher;

    @Test
    void shouldDelegateToPublisherWhenEventReceived() {
        Video video = Video.builder().title("Aula 1").build();

        dispatcher.onVideoPublished(new VideoPublishedEvent(video));

        then(videoEventPublisher).should().publishVideoPublished(video);
    }
}