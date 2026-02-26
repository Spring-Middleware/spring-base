package io.github.spring.middleware.mongo.search;

import io.github.spring.middleware.mongo.annotations.MongoClazzRef;
import io.github.spring.middleware.mongo.annotations.MongoConcatProperty;
import io.github.spring.middleware.mongo.annotations.MongoSearchProperty;
import io.github.spring.middleware.mongo.search.MongoSearch;
import io.github.spring.middleware.mongo.types.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@MongoClazzRef("io.github.spring.middleware.mongo.data.TestItem")
public class TestItemSearch implements MongoSearch {

    @MongoSearchProperty
    private String propItemA;

    @MongoSearchProperty
    private String propItemB;

    @MongoSearchProperty(operationType = OperationType.LIKE)
    @MongoConcatProperty(value = "propA", concat = "propB")
    private String propAB;

}
