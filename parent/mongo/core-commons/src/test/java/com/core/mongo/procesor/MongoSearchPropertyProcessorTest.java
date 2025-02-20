package com.core.mongo.procesor;

import com.core.mongo.processor.MongoAnnotationProcessorParameters;
import com.core.mongo.processor.MongoSearchPropertyProcessor;

public class MongoSearchPropertyProcessorTest {

    private MongoSearchPropertyProcessor mongoSearchPropertyProcessor = new MongoSearchPropertyProcessor();

    public void execute(MongoAnnotationProcessorParameters parameters) throws Exception {

        mongoSearchPropertyProcessor.processAnnotation(parameters);
    }

}
