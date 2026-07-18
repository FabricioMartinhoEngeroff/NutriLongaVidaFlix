package com.dvFabricio.VidaLongaFlix.infra.messaging;

import com.dvFabricio.VidaLongaFlix.services.interaction.NotificationService;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VideoPublisherConfig {

    @Bean
    @ConditionalOnProperty(name = "aws.sqs.video-published-queue-url")
    public VideoEventPublisher sqsVideoEventPublisher(
            SqsTemplate sqsTemplate,
            @Value("${aws.sqs.video-published-queue-url}") String queueUrl) {
        return new SqsVideoEventPublisher(sqsTemplate, queueUrl);
    }

    @Bean
    @ConditionalOnMissingBean(VideoEventPublisher.class)
    public VideoEventPublisher syncVideoEventPublisher(NotificationService notificationService) {
        return new SyncVideoEventPublisher(notificationService);
    }
}
