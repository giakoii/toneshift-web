package vn.toneshift.api;

import org.junit.jupiter.api.Test;
import vn.toneshift.api.support.AbstractIntegrationTest;

/**
 * Smoke test verifying the application context starts against a real database.
 */
class ToneShiftApplicationTests extends AbstractIntegrationTest {

    /**
     * Passes if the full Spring context loads without errors.
     */
    @Test
    void contextLoads() {
    }
}
