package com.core.mongo.search;

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
@NoArgsConstructor
@AllArgsConstructor
public class TestSubDataSearch implements MongoSearch {

    @MongoSearchProperty
    private String propA;
    @MongoSearchProperty
    private String propB;
    @MongoSearchProperty(operationType = OperationType.LIKE)
    @MongoConcatProperty(value = "propA", concat = "propB")
    private String propAB;

}
