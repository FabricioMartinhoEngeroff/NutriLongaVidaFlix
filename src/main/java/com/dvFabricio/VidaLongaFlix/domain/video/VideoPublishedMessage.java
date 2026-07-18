package com.dvFabricio.VidaLongaFlix.domain.video;

import java.util.UUID;

public record VideoPublishedMessage(UUID videoId, String videoTitle) {}
