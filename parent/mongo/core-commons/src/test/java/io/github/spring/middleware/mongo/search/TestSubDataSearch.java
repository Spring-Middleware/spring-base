package io.github.spring.middleware.mongo.search;

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
