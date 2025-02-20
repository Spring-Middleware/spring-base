package com.core.view;

import com.google.common.base.Predicate;
import reactor.util.context.ContextView;

public interface ContextFilterPredicate<T> extends Predicate<T> {

     void applyContext(ContextView contextView);

}
