package com.middleware.error;

import com.middleware.error.api.ErrorView;

public interface Recoverable {

    void recover(ErrorView errorView);

}
