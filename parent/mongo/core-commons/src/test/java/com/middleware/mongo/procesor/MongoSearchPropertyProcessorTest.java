package com.middleware.mongo.procesor;

import com.middleware.mongo.processor.MongoAnnotationProcessorParameters;
import com.middleware.mongo.processor.MongoSearchPropertyProcessor;

public class MongoSearchPropertyProcessorTest {

    private MongoSearchPropertyProcessor mongoSearchPropertyProcessor = new MongoSearchPropertyProcessor();

    public void execute(MongoAnnotationProcessorParameters parameters) throws Exception {

        mongoSearchPropertyProcessor.processAnnotation(parameters);
    }

}
