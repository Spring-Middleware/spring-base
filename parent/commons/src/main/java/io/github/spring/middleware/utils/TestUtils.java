package io.github.spring.middleware.utils;

import org.apache.commons.io.FileUtils;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.File;
import java.nio.charset.Charset;

public class TestUtils {

    public static void copyJsonAndCheck(String json, String filename, boolean copy) {

        if (copy) {
            copyJsonToFile(json, filename);
        }
        checkJson(json, filename);
    }

    public static void copyJsonToFile(String json, String filename) throws RuntimeException {

        try {
            File file = new File("src/test/resources/" + filename);
            FileUtils.writeStringToFile(file, json, Charset.defaultCharset());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void checkJson(String json, String filename) throws RuntimeException {

        try {
            File file = new File("src/test/resources/" + filename);
            JSONAssert.assertEquals(json, FileUtils.readFileToString(file, Charset.defaultCharset()), false);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
