package com.core.mongo.components;

import com.core.mongo.procesor.MongoSearchClassProcessorTest;
import com.core.mongo.procesor.MongoSearchPropertiesProcessorTest;
import com.core.mongo.procesor.MongoSearchPropertyProcessorTest;
import com.core.mongo.processor.MongoAnnotationProcessorParameters;
import com.core.mongo.processor.MongoSearchClassProcessor;
import com.core.mongo.processor.MongoSearchPropertiesProcessor;
import com.core.mongo.processor.MongoSearchPropertyProcessor;
import com.core.mongo.search.MongoSearch;
import com.core.mongo.search.TestDataSearch;
import com.core.mongo.search.TestItemSearch;
import com.core.mongo.search.TestSubDataSearch;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.collections.Sets;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.nio.charset.Charset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;

@Slf4j
public class CriteriaBuilderComponentTest<S extends MongoSearch> {

    @Mock
    private MongoSearchPropertyProcessor searchPropertyProcessor;
    @Mock
    private MongoSearchClassProcessor searchClassProcessor;
    @Mock
    private MongoSearchPropertiesProcessor searchPropertiesProcessor;

    private MongoSearchPropertyProcessorTest mongoSearchPropertyProcessorTest = new MongoSearchPropertyProcessorTest();
    private MongoSearchClassProcessorTest mongoSearchClassProcessorTest = new MongoSearchClassProcessorTest();
    private MongoSearchPropertiesProcessorTest mongoSearchPropertiesProcessorTest = new MongoSearchPropertiesProcessorTest();

    @InjectMocks
    private CriteriaBuilderComponent criteriaBuilderComponent = new CriteriaBuilderComponent();

    @Before
    public void setUp() {

        try {
            MockitoAnnotations.initMocks(this);
            mongoSearchClassProcessorTest.setUp(this);
            mongoSearchPropertiesProcessorTest.setUp();

            doAnswer(invocationOnMock -> {
                mongoSearchPropertyProcessorTest.execute(invocationOnMock.getArgument(0));
                return null;
            }).when(searchPropertyProcessor).processAnnotation(any(MongoAnnotationProcessorParameters.class));

            doAnswer(invocationOnMock -> {
                mongoSearchClassProcessorTest.execute(invocationOnMock.getArgument(0));
                return null;
            }).when(searchClassProcessor)
                    .processAnnotation(any(MongoAnnotationProcessorParameters.class));

            doCallRealMethod().when(searchPropertiesProcessor)
                    .processAnnotation(any(MongoAnnotationProcessorParameters.class));

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @Test
    public void testMongoSearchProperty_KEY() throws Exception {

        TestDataSearch dataSearch = TestDataSearch.builder()
                .key("KEY").build();

        Criteria criteria = buildCriteria((S) dataSearch, new Criteria(), "");
        Query query = Query.query(criteria);
        assertJson(query.getQueryObject().toJson(), "TEST_KEY.json");

    }

    @Test
    public void testMongoSearchProperty_KEY_VALUE() throws Exception {

        TestDataSearch dataSearch = TestDataSearch.builder()
                .key("KEY")
                .value("VALUE").build();

        Criteria criteria = buildCriteria((S) dataSearch, new Criteria(), "");
        Query query = Query.query(criteria);
        assertJson(query.getQueryObject().toJson(), "TEST_KEY_VALUE.json");
    }

    @Test
    public void testMongoSearchProperty_KEY_NAME() throws Exception {

        TestDataSearch dataSearch = TestDataSearch.builder()
                .key("KEY")
                .naming("NAME").build();

        Criteria criteria = buildCriteria((S) dataSearch, new Criteria(), "");
        Query query = Query.query(criteria);
        assertJson(query.getQueryObject().toJson(), "TEST_KEY_NAME.json");

    }

    @Test
    public void testMongoSearchClass() throws Exception {

        TestDataSearch dataSearch = TestDataSearch.builder()
                .key("KEY")
                .subSearch(TestSubDataSearch.builder()
                        .propA("PROP_A")
                        .propB("PROP_B")
                        .build())
                .build();
        Criteria criteria = buildCriteria((S) dataSearch, new Criteria(), "");
        Query query = Query.query(criteria);
        assertJson(query.getQueryObject().toJson(), "TEST_CLASS.json");
    }

    @Test
    public void testMongoSearchProperty_ORA_ORB() throws Exception {

        TestDataSearch dataSearch = TestDataSearch.builder()
                .orA("OR_A")
                .orB("OR_B")
                .build();

        Criteria criteria = buildCriteria((S) dataSearch, new Criteria(), "");
        Query query = Query.query(criteria);
        assertJson(query.getQueryObject().toJson(), "TEST_OR.json");
    }

    @Test
    public void testMongoSearchProperty_KEY_ORA_ORB() throws Exception {

        TestDataSearch dataSearch = TestDataSearch.builder()
                .key("KEY")
                .orA("OR_A")
                .orB("OR_B")
                .build();

        Criteria criteria = buildCriteria((S) dataSearch, new Criteria(), "");
        Query query = Query.query(criteria);
        assertJson(query.getQueryObject().toJson(), "TEST_KEY_OR.json");
    }

    @Test
    public void testMongoSearchClass_ITEM() throws Exception {

        TestDataSearch dataSearch = TestDataSearch.builder()
                .itemSearch(TestItemSearch.builder()
                        .propItemA("PROP_ITEM_A")
                        .propItemB("PROP_ITEM_B")
                        .build())
                .build();

        Criteria criteria = buildCriteria((S) dataSearch, new Criteria(), "");
        Query query = Query.query(criteria);
        assertJson(query.getQueryObject().toJson(), "TEST_ITEM.json");

    }

    @Test
    public void testMongoSearchClass_ITEMS() throws Exception {

        TestDataSearch dataSearch = TestDataSearch.builder()
                .itemsSearch(Sets.newSet(TestItemSearch.builder()
                        .propItemA("PROP_ITEM_A1")
                        .propItemB("PROP_ITEM_B1")
                        .build(), TestItemSearch.builder()
                        .propItemA("PROP_ITEM_A2")
                        .propItemB("PROP_ITEM_B2").build())).build();


        Criteria criteria = buildCriteria((S) dataSearch, new Criteria(), "");
        Query query = Query.query(criteria);
        assertJson(query.getQueryObject().toJson(), "TEST_ITEMS.json");

    }

    private void assertJson(String json, String resourceName) throws Exception {

        JSONAssert.assertEquals(IOUtils
                .toString(this.getClass().getClassLoader()
                                .getResourceAsStream(resourceName),
                        Charset.defaultCharset()), json, false);
    }

    public Criteria buildCriteria(S mongoSearch, Criteria criteria, String path) throws Exception {

        return criteriaBuilderComponent.buildCriteria(mongoSearch, criteria, path);
    }

}
