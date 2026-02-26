package com.middleware.util;

import com.middleware.annotation.MappedParam;
import lombok.Data;

@Data
public class ParamsTest extends Parametrized {

    @MappedParam
    private String paramTest;

}
