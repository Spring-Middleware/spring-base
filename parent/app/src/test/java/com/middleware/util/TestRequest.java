package com.middleware.util;

import com.middleware.annotation.MappedClass;
import com.middleware.annotation.MappedParam;
import lombok.Data;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

@Data
public class TestRequest<P extends Parametrized> {

    @MappedParam
    private String text;

    @MappedParam
    private Integer id;

    @MappedParam
    private UUID uuid;

    @MappedParam
    private List<Integer> ids;

    @MappedParam(value = "fecha")
    private Date date;
    @MappedParam
    private List<String> codes;

    @MappedClass(params = {"adult", "children"})
    private PriceTypeRequest priceTypeRequest;

    @MappedParam
    private PriceType priceType;

    @MappedClass
    private P parametrized;

    public void setCodes(List<String> codes) {
        this.codes = codes;
    }
}
