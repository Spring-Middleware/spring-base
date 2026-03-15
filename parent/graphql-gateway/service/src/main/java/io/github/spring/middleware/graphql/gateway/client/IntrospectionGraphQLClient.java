package io.github.spring.middleware.graphql.gateway.client;

import graphql.introspection.IntrospectionResultToSchema;
import graphql.schema.idl.SchemaPrinter;
import io.github.spring.middleware.client.proxy.UrlJoiner;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;
import io.github.spring.middleware.registry.model.SchemaLocation;
import io.github.spring.middleware.util.WebClientUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static io.github.spring.middleware.client.proxy.UrlJoiner.join;
import static io.github.spring.middleware.utils.EndpointUtils.joinUrl;
import static io.github.spring.middleware.utils.EndpointUtils.normalizeContextPath;
import static io.github.spring.middleware.utils.EndpointUtils.normalizeEndpoint;
import static io.github.spring.middleware.utils.EndpointUtils.normalizePath;

@Component
public class IntrospectionGraphQLClient {

    private WebClient webClient = WebClientUtils.createWebClient(1000, 5);
    private IntrospectionResultToSchema resultToSchema = new IntrospectionResultToSchema();
    private SchemaPrinter schemaPrinter = new SchemaPrinter();

    private static final String INTROSPECTION_QUERY = """
            query IntrospectionQuery {
              __schema {
                queryType { name }
                mutationType { name }
                subscriptionType { name }
                types {
                  kind
                  name
                  description
                  fields(includeDeprecated: true) {
                    name
                    description
                    args {
                      name
                      description
                      defaultValue
                      type {
                        kind
                        name
                        ofType {
                          kind
                          name
                          ofType {
                            kind
                            name
                          }
                        }
                      }
                    }
                    type {
                      kind
                      name
                      ofType {
                        kind
                        name
                        ofType {
                          kind
                          name
                        }
                      }
                    }
                    isDeprecated
                    deprecationReason
                  }
                  inputFields {
                    name
                    description
                    defaultValue
                    type {
                      kind
                      name
                      ofType {
                        kind
                        name
                        ofType {
                          kind
                          name
                        }
                      }
                    }
                  }
                  interfaces {
                    kind
                    name
                    ofType {
                      kind
                      name
                    }
                  }
                  enumValues(includeDeprecated: true) {
                    name
                    description
                    isDeprecated
                    deprecationReason
                  }
                  possibleTypes {
                    kind
                    name
                    ofType {
                      kind
                      name
                    }
                  }
                }
                directives {
                  name
                  description
                  locations
                  args {
                    name
                    description
                    defaultValue
                    type {
                      kind
                      name
                      ofType {
                        kind
                        name
                        ofType {
                          kind
                          name
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

    public String fetchRemoteSchema(SchemaLocation schemaLocation) {
        final String clusterEndpoint = joinUrl(normalizeEndpoint(schemaLocation.getLocation()), normalizeContextPath(schemaLocation.getContextPath()));
        final String graphqlEndpoint = join(clusterEndpoint, normalizePath(schemaLocation.getPathApi()));
        try {
            Map<String, Object> response = webClient.post()
                    .uri(graphqlEndpoint)
                    .bodyValue(Map.of("query", INTROSPECTION_QUERY))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return schemaPrinter.print(resultToSchema.createSchemaDefinition((Map)response.get("data")));

        } catch (Exception e) {
            throw new GraphQLException(GraphQLErrorCodes.SCHEMA_FETCH_ERROR, STR."Failed to fetch remote schema from \{graphqlEndpoint}", e);
        }
    }

}
