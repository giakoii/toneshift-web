package vn.toneshift.api.common.dto;

import java.util.UUID;

/**
 * Minimal public reference to a user, embedded in other responses.
 *
 * @param id    user identifier
 * @param name  display name
 * @param image avatar image URL, may be null
 */
public record UserRef(UUID id, String name, String image) {
}
