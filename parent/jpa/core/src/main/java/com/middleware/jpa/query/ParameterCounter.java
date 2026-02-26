package com.middleware.jpa.query;

public class ParameterCounter {

    public int counter = 0;

    public String next() {

        return "param" + counter++;
    }

    public String actual() {

        return "param" + counter;
    }

}
