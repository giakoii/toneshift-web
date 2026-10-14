package vn.toneshift.api.common.enums;

/**
 * Account status, stored as a string in {@code users.status}.
 *
 * <p>A {@code BLOCKED} account is refused at login and on refresh.
 */
public enum UserStatus {

    ACTIVE,
    BLOCKED
}
