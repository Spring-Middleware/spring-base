package com.core.mongo.components;

import com.core.mongo.search.TestDataSearch;
import com.core.mongo.search.TestItemSearch;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;

import java.nio.charset.Charset;

public class AddFieldsBuilderComponentTest {

    private AddFieldsBuilderComponent addFieldsBuilderComponent = new AddFieldsBuilderComponent();

    @Test
    public void testConcatProperty() throws Exception {

        TestDataSearch dataSearch = TestDataSearch.builder()
                .itemSearch(TestItemSearch.builder()
                        .propAB("PROP_AB")
                        .build()).build();

        AddFieldsOperation addFieldsOperation = addFieldsBuilderComponent.buildAddFields(dataSearch);
        assertJson(addFieldsOperation.toDocument(Aggregation.DEFAULT_CONTEXT).toJson(),"TEST_CONCAT.json");
    }

    private void assertJson(String json, String resourceName) throws Exception {

        JSONAssert.assertEquals(IOUtils
                .toString(this.getClass().getClassLoader()
                                .getResourceAsStream(resourceName),
                        Charset.defaultCharset()), json, false);
    }


}
