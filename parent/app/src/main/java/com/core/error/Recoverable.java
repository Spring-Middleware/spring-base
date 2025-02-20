package com.core.error;

import com.core.error.api.ErrorView;

public interface Recoverable {

    void recover(ErrorView errorView);

}
