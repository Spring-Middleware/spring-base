package io.github.spring.middleware.graphql.gateway.util;

import graphql.language.UnionTypeDefinition;
import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.RuntimeWiring;
import io.github.spring.middleware.graphql.config.GraphQLLinkArgumentsScalar;
import io.github.spring.middleware.graphql.gateway.scalars.InstantScalar;
import io.github.spring.middleware.graphql.gateway.scalars.MapStringObjectScalar;
import io.github.spring.middleware.graphql.gateway.scalars.OffsetDateTimeScalar;
import io.github.spring.middleware.graphql.gateway.scalars.ScalarsProvider;
import io.github.spring.middleware.graphql.gateway.scalars.URIScalar;

import java.util.Optional;

public class RuntimeWiringUtils {

    public static void registerScalars(RuntimeWiring.Builder builder, Optional<ScalarsProvider> scalarsProviderOptional) {
        builder
                .scalar(ExtendedScalars.UUID)
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.Time)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.CountryCode)
                .scalar(ExtendedScalars.Currency)
                .scalar(ExtendedScalars.Locale)
                .scalar(ExtendedScalars.LocalTime)
                .scalar(ExtendedScalars.Url)
                .scalar(InstantScalar.INSTANCE)
                .scalar(OffsetDateTimeScalar.INSTANCE)
                .scalar(URIScalar.INSTANCE)
                .scalar(GraphQLLinkArgumentsScalar.INSTANCE);


        scalarsProviderOptional.ifPresent(provider -> provider.getScalars().forEach(builder::scalar));
    }

    public static void registerResolver(RuntimeWiring.Builder builder, UnionTypeDefinition unionTypeDefinition) {
        builder.type(unionTypeDefinition.getName(), typeBuilder -> {
            typeBuilder.typeResolver(GraphQLTypeResolvers.fromUnionDefinition(unionTypeDefinition));
            return typeBuilder;
        });
    }

}
