package com.dvFabricio.VidaLongaFlix.infra.messaging;

import com.dvFabricio.VidaLongaFlix.domain.video.VideoPublishedMessage;
import com.dvFabricio.VidaLongaFlix.services.interaction.NotificationService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aws.sqs.video-published-queue-url")
public class VideoPublishedListener {

    private final NotificationService notificationService;

    public VideoPublishedListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @SqsListener("${aws.sqs.video-published-queue-url}")
    public void onVideoPublished(VideoPublishedMessage message) {
        notificationService.createForVideo(message.videoId(), message.videoTitle());
    }
}
