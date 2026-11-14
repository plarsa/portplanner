package se.portplanner.module;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the module to be enabled and the signed-in role to have a sufficient level. Without
 * {@code level}, {@code read} is required for GET/HEAD/OPTIONS and {@code write} for every other method.
 * It can sit on both the class and the method; both are checked.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequiresModule {
    String value();

    /** {@link AccessLevel#NONE} means derive the level from the HTTP method. */
    AccessLevel level() default AccessLevel.NONE;
}
