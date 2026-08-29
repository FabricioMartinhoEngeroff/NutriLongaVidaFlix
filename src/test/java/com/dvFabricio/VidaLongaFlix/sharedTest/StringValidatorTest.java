package com.dvFabricio.VidaLongaFlix.sharedTest;

import com.dvFabricio.VidaLongaFlix.domain.shared.StringValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringValidatorTest {

    @Test
    void isBlankShouldReturnTrueForNull() {
        assertTrue(StringValidator.isBlank(null));
    }

    @Test
    void isBlankShouldReturnTrueForEmptyString() {
        assertTrue(StringValidator.isBlank(""));
    }

    @Test
    void isBlankShouldReturnTrueForOnlySpaces() {
        assertTrue(StringValidator.isBlank("   "));
    }

    @Test
    void isBlankShouldReturnFalseForNonBlank() {
        assertFalse(StringValidator.isBlank("texto"));
    }

    @Test
    void isNotBlankShouldReturnTrueForNonBlank() {
        assertTrue(StringValidator.isNotBlank("texto"));
    }

    @Test
    void isNotBlankShouldReturnFalseForNull() {
        assertFalse(StringValidator.isNotBlank(null));
    }

    @Test
    void isNotBlankShouldReturnFalseForBlankString() {
        assertFalse(StringValidator.isNotBlank("  "));
    }
}
