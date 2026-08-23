package com.dvFabricio.VidaLongaFlix.infra.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Component
public class VideoEventDispatcher {

    private final VideoEventPublisher videoEventPublisher;

    public VideoEventDispatcher(VideoEventPublisher videoEventPublisher) {
        this.videoEventPublisher = videoEventPublisher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVideoPublished(VideoPublishedEvent event) {
        videoEventPublisher.publishVideoPublished(event.video());
    }
}