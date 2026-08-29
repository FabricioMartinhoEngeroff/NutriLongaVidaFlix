package com.dvFabricio.VidaLongaFlix.sharedTest;

import com.dvFabricio.VidaLongaFlix.domain.shared.ErrorMessages;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ErrorMessagesTest {

    private final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void userNotFoundShouldContainId() {
        String msg = ErrorMessages.userNotFound(id);
        assertTrue(msg.contains(id.toString()));
    }

    @Test
    void videoNotFoundShouldContainId() {
        String msg = ErrorMessages.videoNotFound(id);
        assertTrue(msg.contains(id.toString()));
    }

    @Test
    void categoryNotFoundShouldContainId() {
        String msg = ErrorMessages.categoryNotFound(id);
        assertTrue(msg.contains(id.toString()));
    }

    @Test
    void commentNotFoundShouldContainId() {
        String msg = ErrorMessages.commentNotFound(id);
        assertTrue(msg.contains(id.toString()));
    }

    @Test
    void menuNotFoundShouldContainId() {
        String msg = ErrorMessages.menuNotFound(id);
        assertTrue(msg.contains(id.toString()));
    }
}
