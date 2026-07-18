package com.dvFabricio.VidaLongaFlix.infra.messaging;

import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.services.interaction.NotificationService;

public class SyncVideoEventPublisher implements VideoEventPublisher {

    private final NotificationService notificationService;

    public SyncVideoEventPublisher(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void publishVideoPublished(Video video) {
        notificationService.createForVideo(video);
    }
}
