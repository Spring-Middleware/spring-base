package com.core.util;

import com.core.annotation.MappedParam;
import lombok.Data;

@Data
public class ParamsTest extends Parametrized {

    @MappedParam
    private String paramTest;

}
