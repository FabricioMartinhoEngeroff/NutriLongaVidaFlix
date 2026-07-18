package com.dvFabricio.VidaLongaFlix.infra.messaging;

import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.domain.video.VideoPublishedMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;

public class SqsVideoEventPublisher implements VideoEventPublisher {

    private final SqsTemplate sqsTemplate;
    private final String queueUrl;

    public SqsVideoEventPublisher(SqsTemplate sqsTemplate, String queueUrl) {
        this.sqsTemplate = sqsTemplate;
        this.queueUrl = queueUrl;
    }

    @Override
    public void publishVideoPublished(Video video) {
        sqsTemplate.send(queueUrl, new VideoPublishedMessage(video.getId(), video.getTitle()));
    }
}
