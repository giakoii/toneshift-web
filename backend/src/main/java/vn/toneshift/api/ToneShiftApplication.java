package vn.toneshift.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * Entry point of the ToneShift backend application.
 */
@SpringBootApplication
public class ToneShiftApplication {

    /**
     * Forces the JVM default time zone to UTC and starts the Spring application.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(ToneShiftApplication.class, args);
    }
}
