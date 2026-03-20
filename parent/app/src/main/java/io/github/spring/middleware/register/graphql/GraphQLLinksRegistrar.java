package io.github.spring.middleware.register.graphql;

import io.github.spring.middleware.annotations.EnableGraphQLLinks;
import io.github.spring.middleware.graphql.config.GraphQLLinksConfiguration;
import io.github.spring.middleware.graphql.config.GraphQLLinksProperties;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;


public class GraphQLLinksRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {

        Map<String, Object> attributes =
                importingClassMetadata.getAnnotationAttributes(EnableGraphQLLinks.class.getName());

        String[] basePackages = attributes != null
                ? (String[]) attributes.get("basePackages")
                : new String[0];

        BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.genericBeanDefinition(GraphQLLinksProperties.class);

        MutablePropertyValues propertyValues = builder.getBeanDefinition().getPropertyValues();
        propertyValues.add("basePackages", basePackages);

        registry.registerBeanDefinition("graphQLLinksProperties", builder.getBeanDefinition());
        registry.registerBeanDefinition(
                "graphQLLinksConfiguration",
                BeanDefinitionBuilder.genericBeanDefinition(
                        io.github.spring.middleware.graphql.config.GraphQLLinksConfiguration.class
                ).getBeanDefinition()
        );
    }
}
