package vn.toneshift.api.common.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events, so services depend on this instead of Spring's publisher directly.
 */
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher delegate;

    /**
     * Publishes an event to its listeners.
     *
     * @param event the event to publish
     */
    public void publish(DomainEvent event) {
        delegate.publishEvent(event);
    }
}
