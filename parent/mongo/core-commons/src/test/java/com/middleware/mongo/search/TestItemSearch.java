package com.middleware.mongo.search;

import com.middleware.mongo.annotations.MongoClazzRef;
import com.middleware.mongo.annotations.MongoConcatProperty;
import com.middleware.mongo.annotations.MongoSearchProperty;
import com.middleware.mongo.types.OperationType;
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
@MongoClazzRef("com.middleware.mongo.data.TestItem")
public class TestItemSearch implements MongoSearch {

    @MongoSearchProperty
    private String propItemA;

    @MongoSearchProperty
    private String propItemB;

    @MongoSearchProperty(operationType = OperationType.LIKE)
    @MongoConcatProperty(value = "propA", concat = "propB")
    private String propAB;

}
