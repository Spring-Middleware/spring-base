package com.common.view;

import com.core.view.View;

import java.util.Collection;
import java.util.function.Function;

public class FillerFunction<E extends View, R extends View> {

    private String functionName;
    private Collection<SupplierId<E>> suppliersId;
    private Function<R, Integer> responsesId;
    private Function<Collection<Integer>, Collection<R>> fillerFunction;

    public Collection<SupplierId<E>> getSuppliersId() {

        return suppliersId;
    }

    public void setSuppliersId(Collection<SupplierId<E>> suppliersId) {

        this.suppliersId = suppliersId;
    }

    public Function<R, Integer> getResponseId() {

        return responsesId;
    }

    public void setResponsesId(Function<R, Integer> responsesId) {

        this.responsesId = responsesId;
    }

    public String getFunctionName() {

        return functionName;
    }

    public void setFunctionName(String functionName) {

        this.functionName = functionName;
    }

    public Function<Collection<Integer>, Collection<R>> getFillerFunction() {

        return fillerFunction;
    }

    public void setFillerFunction(Function<Collection<Integer>, Collection<R>> fillerFunction) {

        this.fillerFunction = fillerFunction;
    }
}
