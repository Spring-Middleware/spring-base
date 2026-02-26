package io.github.spring.middleware.mongo.procesor;

import io.github.spring.middleware.mongo.components.CriteriaBuilderComponent;
import io.github.spring.middleware.mongo.components.CriteriaBuilderComponentTest;
import io.github.spring.middleware.mongo.processor.MongoAnnotationProcessorParameters;
import io.github.spring.middleware.mongo.processor.MongoSearchClassProcessor;
import io.github.spring.middleware.mongo.search.MongoSearch;
import lombok.extern.slf4j.Slf4j;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.query.Criteria;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
public class MongoSearchClassProcessorTest {

    @InjectMocks
    private MongoSearchClassProcessor mongoSearchClassProcessor;

    @Mock
    private CriteriaBuilderComponent criteriaBuilderComponent;

    private CriteriaBuilderComponentTest criteriaBuilderComponentTest;

    public void setUp(CriteriaBuilderComponentTest criteriaBuilderComponentTest) {

        try {
            MockitoAnnotations.initMocks(this);
            this.criteriaBuilderComponentTest = criteriaBuilderComponentTest;
            when(criteriaBuilderComponent.buildCriteria(any(MongoSearch.class), any(Criteria.class), anyString()))
                    .thenAnswer(invocationOnMock -> {
                        return criteriaBuilderComponentTest
                                .buildCriteria(invocationOnMock.getArgument(0), invocationOnMock.getArgument(1),
                                        invocationOnMock.getArgument(2));

                    });
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public void execute(MongoAnnotationProcessorParameters parameters) throws Exception {

        mongoSearchClassProcessor.processAnnotation(parameters);
    }

}
