package com.core.mongo.search;

import com.core.mongo.annotations.MongoClazzRef;
import com.core.mongo.annotations.MongoConcatProperty;
import com.core.mongo.annotations.MongoSearchProperty;
import com.core.mongo.types.OperationType;
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
@MongoClazzRef("com.core.mongo.data.TestItem")
public class TestItemSearch implements MongoSearch {

    @MongoSearchProperty
    private String propItemA;

    @MongoSearchProperty
    private String propItemB;

    @MongoSearchProperty(operationType = OperationType.LIKE)
    @MongoConcatProperty(value = "propA", concat = "propB")
    private String propAB;

}
