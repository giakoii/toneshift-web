package vn.toneshift.api.common.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogMaskerTest {

    @Test
    void maskEmailKeepsFirstCharacterAndDomain() {
        assertThat(LogMasker.maskEmail("khoi@example.com")).isEqualTo("k***@example.com");
    }

    @Test
    void maskEmailReturnsMaskWhenLocalPartIsMissing() {
        assertThat(LogMasker.maskEmail("@example.com")).isEqualTo("***");
    }

    @Test
    void maskEmailReturnsMaskWhenValueIsBlankOrHasNoDomain() {
        assertThat(LogMasker.maskEmail("  ")).isEqualTo("***");
        assertThat(LogMasker.maskEmail(null)).isEqualTo("***");
        assertThat(LogMasker.maskEmail("khoi")).isEqualTo("***");
    }

    @Test
    void maskTokenKeepsOnlyLastFourCharacters() {
        assertThat(LogMasker.maskToken("abcdefghijkl")).isEqualTo("***ijkl");
    }

    @Test
    void maskTokenHidesShortValuesEntirely() {
        assertThat(LogMasker.maskToken("abcd")).isEqualTo("***");
        assertThat(LogMasker.maskToken(null)).isEqualTo("***");
    }
}
