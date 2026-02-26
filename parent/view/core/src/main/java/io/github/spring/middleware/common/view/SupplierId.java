package io.github.spring.middleware.common.view;

import java.util.function.Function;

public class SupplierId<E> {

    private Function<E, Integer> supplierFunction;
    private String supplierName;

    public SupplierId(Function<E, Integer> supplierFunction) {

        this.supplierFunction = supplierFunction;
    }

    public SupplierId(Function<E, Integer> supplierFunction, String supplierName) {

        this.supplierFunction = supplierFunction;
        this.supplierName = supplierName;
    }

    public Function<E, Integer> getSupplierFunction() {

        return supplierFunction;
    }

    public void setSupplierFunction(Function<E, Integer> supplierFunction) {

        this.supplierFunction = supplierFunction;
    }

    public String getSupplierName() {

        return supplierName;
    }

    public void setSupplierName(String supplierName) {

        this.supplierName = supplierName;
    }

}
