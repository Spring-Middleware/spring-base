package io.github.spring.middleware.view.annotations;

import com.google.common.base.Predicate;
import io.github.spring.middleware.view.ContextFilterPredicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import reactor.util.context.ContextView;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ViewProperty {

    String value() default "";

    Class<? extends Predicate> filter() default NoFilter.class;

    Class<? extends ContextFilterPredicate> filterContext() default NoFilterContext.class;

    Class<? extends Supplier> applyIncludeProperties() default IncludePropertiesSupplier.class;

    String[] excludeProperties() default {};

    String[] includeProperties() default {};

    class NoFilter implements Predicate {

        public boolean apply(Object obejct) {

            return true;
        }
    }

    class NoFilterContext implements ContextFilterPredicate {

        @Override
        public void applyContext(ContextView contextView) {

        }

        @Override
        public boolean apply(@Nullable Object o) {

            return true;
        }
    }

    class IncludePropertiesSupplier implements Supplier<Boolean> {

        public Boolean get() {

            return Boolean.TRUE;
        }
    }
}
