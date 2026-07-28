package simulation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks types that assume single-threaded access.
 *
 * <p>See {@code docs/THREADING.md}. Callers must not mutate annotated types from
 * background threads without an external serialization strategy (e.g. a single
 * game thread with message passing).
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface NotThreadSafe {
}
