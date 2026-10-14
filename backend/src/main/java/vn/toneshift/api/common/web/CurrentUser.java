package vn.toneshift.api.common.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the id of the authenticated caller into a controller method.
 *
 * <p>Declare the parameter as {@code UUID} on endpoints that require a login, or as
 * {@code Optional<UUID>} on public endpoints that behave differently for a signed-in caller.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
