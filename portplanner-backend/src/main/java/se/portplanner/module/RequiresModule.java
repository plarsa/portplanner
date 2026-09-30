package se.portplanner.module;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kräver att modulen är aktiverad och att inloggad roll har tillräcklig nivå. Utan {@code level} krävs
 * {@code read} för GET/HEAD/OPTIONS och {@code write} för övriga metoder. Kan sitta på klass och metod;
 * båda kontrolleras.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequiresModule {
    String value();

    /** {@link AccessLevel#NONE} = härled från HTTP-metod. */
    AccessLevel level() default AccessLevel.NONE;
}
