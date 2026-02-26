package io.github.spring.middleware.error;

import io.github.spring.middleware.error.api.ErrorView;

public interface Recoverable {

    void recover(ErrorView errorView);

}
