package io.github.spring.middleware.mongo.procesor;

import io.github.spring.middleware.mongo.processor.MongoAnnotationProcessorParameters;
import io.github.spring.middleware.mongo.processor.MongoSearchPropertyProcessor;

public class MongoSearchPropertyProcessorTest {

    private MongoSearchPropertyProcessor mongoSearchPropertyProcessor = new MongoSearchPropertyProcessor();

    public void execute(MongoAnnotationProcessorParameters parameters) throws Exception {

        mongoSearchPropertyProcessor.processAnnotation(parameters);
    }

}
