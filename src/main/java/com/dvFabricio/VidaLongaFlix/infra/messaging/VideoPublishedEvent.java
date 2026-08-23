package com.dvFabricio.VidaLongaFlix.infra.messaging;

import com.dvFabricio.VidaLongaFlix.domain.video.Video;


public record VideoPublishedEvent(Video video) {}