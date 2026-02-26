package com.middleware.mongo.processor;

public interface MongoAnnotationProcessor<P extends MongoAnnotationProcessorParameters> {

    void processAnnotation(P mongoAnnotationProcessorParameters) throws Exception;

}
