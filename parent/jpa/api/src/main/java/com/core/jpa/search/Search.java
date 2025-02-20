package com.core.jpa.search;


import jakarta.persistence.LockModeType;

import java.io.Serializable;

public interface Search extends Serializable {

    default LockModeType lockModeType() {
        return null;
    }

}
