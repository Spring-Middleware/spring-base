package com.core.mongo.procesor;

import com.core.mongo.processor.MongoAnnotationProcessor;
import com.core.mongo.processor.MongoAnnotationProcessorParameters;
import com.core.mongo.processor.MongoSearchPropertiesProcessor;
import lombok.extern.slf4j.Slf4j;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@Slf4j
public class MongoSearchPropertiesProcessorTest {

    @Mock
    private MongoAnnotationProcessor searchPropertyProcessor;

    @InjectMocks
    private MongoSearchPropertiesProcessor mongoSearchPropertiesProcessor = new MongoSearchPropertiesProcessor();

    private MongoSearchPropertyProcessorTest mongoSearchPropertyProcessorTest;


    public void setUp() {

        MockitoAnnotations.initMocks(this);
        try {
            mongoSearchPropertyProcessorTest = new MongoSearchPropertyProcessorTest();
            doAnswer(invocationOnMock -> {
                mongoSearchPropertyProcessorTest.execute(invocationOnMock.getArgument(0));
                return null;
            }).when(searchPropertyProcessor).processAnnotation(any(MongoAnnotationProcessorParameters.class));

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public void execute(MongoAnnotationProcessorParameters parameters) throws Exception {

        mongoSearchPropertiesProcessor.processAnnotation(parameters);
    }

}
