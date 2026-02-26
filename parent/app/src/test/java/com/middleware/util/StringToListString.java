package com.middleware.util;

import com.middleware.view.DataAdaptor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StringToListString implements DataAdaptor<String, List<String>> {

    @Override
    public List<String> adapt(String s) {

        return Arrays.stream(s.split(",")).collect(Collectors.toList());
    }
}
