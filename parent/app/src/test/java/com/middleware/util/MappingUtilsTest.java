package com.middleware.util;

import io.github.spring.middleware.util.MappingUtils;
import org.junit.Test;

import java.sql.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MappingUtilsTest {

    @Test
    public void testMapping() throws Exception {

        Map<String, String[]> requestMap = new HashMap<>();
        requestMap.put("text", Arrays.asList("hola").toArray(new String[1]));
        requestMap.put("id", Arrays.asList("2303").toArray(new String[1]));
        requestMap.put("ids", Arrays.asList("1", "2", "4", "7").toArray(new String[4]));
        requestMap.put("fecha", Arrays.asList("2023-05-10").toArray(new String[1]));

        TestRequest testRequest = MappingUtils.map(requestMap, TestRequest.class);
        assertEquals(testRequest.getText(), "hola");
        assertEquals(testRequest.getIds(), Arrays.asList(1, 2, 4, 7));
        assertEquals(testRequest.getDate(), Date.valueOf("2023-05-10"));
        assertNull(testRequest.getPriceTypeRequest());

    }

    @Test
    public void testMappingAdapter() throws Exception {

        Map<String, String[]> requestMap = new HashMap<>();
        requestMap.put("codes", Arrays.asList("code1","code2","code3").toArray(new String[3]));

        TestRequest testRequest = MappingUtils.map(requestMap, TestRequest.class);
        assertNull(testRequest.getText());
        assertNull(testRequest.getIds());
        assertEquals(testRequest.getCodes(), Arrays.asList("code1", "code2", "code3"));
        assertNull(testRequest.getPriceTypeRequest());

    }

    @Test
    public void testMappingEnum() throws Exception {

        Map<String, String[]> requestMap = new HashMap<>();
        requestMap.put("priceType", Arrays.asList("ADULT").toArray(new String[1]));
        TestRequest testRequest = MappingUtils.map(requestMap, TestRequest.class);
        assertEquals(testRequest.getPriceType(), PriceType.ADULT);
    }

    @Test
    public void testMappingWrongEnum() throws Exception {

        Map<String, String[]> requestMap = new HashMap<>();
        requestMap.put("priceType", Arrays.asList("TEEN").toArray(new String[1]));
        TestRequest testRequest = MappingUtils.map(requestMap, TestRequest.class);
        assertEquals(testRequest.getPriceType(), null);
    }

    @Test
    public void testMappingClass() throws Exception {

        Map<String, String[]> requestMap = new HashMap<>();
        requestMap.put("text", Arrays.asList("hola").toArray(new String[1]));
        requestMap.put("ids", Arrays.asList("1", "2", "4", "7").toArray(new String[4]));
        requestMap.put("adult", Arrays.asList("2").toArray(new String[1]));
        requestMap.put("children", Arrays.asList("2").toArray(new String[1]));

        TestRequest testRequest = MappingUtils.map(requestMap, TestRequest.class);
        assertEquals(testRequest.getText(), "hola");
        assertEquals(testRequest.getIds(), Arrays.asList(1, 2, 4, 7));
        assertEquals(testRequest.getPriceTypeRequest().getAdult().intValue(), 2);
        assertEquals(testRequest.getPriceTypeRequest().getChildren().intValue(), 2);
    }

    @Test
    public void testParametrized() throws Exception {

        Map<String, String[]> requestMap = new HashMap<>();
        requestMap.put("text", Arrays.asList("hola").toArray(new String[1]));
        requestMap.put("adult", Arrays.asList("2").toArray(new String[1]));
        requestMap.put("children", Arrays.asList("2").toArray(new String[1]));
        requestMap.put("paramTest", Arrays.asList("test").toArray(new String[1]));
        TestRequest<ParamsTest> testRequest = MappingUtils.map(requestMap, TestRequest.class, ParamsTest.class);
        assertTrue(testRequest.getParametrized() instanceof ParamsTest);
        assertEquals(testRequest.getParametrized().getParamTest(), "test");
    }

}
