package com.dvFabricio.VidaLongaFlix.infra.messaging;

import com.dvFabricio.VidaLongaFlix.domain.video.Video;

public interface VideoEventPublisher {
    void publishVideoPublished(Video video);
}
