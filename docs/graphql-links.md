# GraphQL Links (GraphQLLink)

This document explains how Spring Middleware implements GraphQL Links using the `@GraphQLLink` family of annotations and how the GraphQL gateway composes queries and resolves linked fields across services.

Overview
--------

GraphQL Links allow one service's schema to declare that a field is resolved by a query living in another service. The platform collects metadata from annotated classes and methods and the gateway uses that metadata to build and execute remote GraphQL queries and compose the result into the final response.

There are two main link styles supported:

- Simple argument mapping (single argument, e.g. `ids`) — the field value is passed directly as the single remote argument.
- Multi-argument mapping — the method returns a map-like structure (or a GraphQLLinkArguments helper) that maps multiple target argument names to values.

Annotations and metadata
------------------------

Key annotations (found in the codebase under `io.github.spring.middleware.annotation.graphql`):

- `@GraphQLLink` — placed on fields or methods to declare a link to another schema. Attributes include:
  - `schema` — target schema namespace
  - `type` — target GraphQL type name
  - `query` — remote query (field) name at the target service
  - `arguments` — array of `@GraphQLLinkArgument` describing mapping of link arguments
  - `collection` — whether the remote query returns a collection

- `@GraphQLLinkArgument` — describes a single argument mapping (name, optional target type)

- `@GraphQLLinkClass` and `@GraphQLType` — class-level markers used by the metadata builder

How metadata is built (implementation pointers)
-----------------------------------------------

During application startup each service that participates in GraphQL scanning builds metadata for linked types. Relevant components:

- `GraphQLLinkedTypeBuilder` — scans classes annotated with `@GraphQLType` / `@GraphQLLinkClass` and collects linked fields/methods.
- `GraphQLFieldLinkDefinitionBuilder` — builds `GraphQLFieldLinkDefinition` instances from fields or methods annotated with `@GraphQLLink`.
- `GraphQLFieldLinkDefinition` — simple POJO that contains: `fieldName`, `targetTypeName`, `schema`, `query`, `argumentLinkDefinitions`, `collection`.

The registry stores `SchemaLocation` records for services; the gateway loads linked-type metadata from each schema and builds the `GraphQLLinkTypesMap` to know which fields are linked and where.

How the gateway resolves links
------------------------------

The gateway composes an executable schema and provides a custom data fetcher for linked fields: `RemoteDelegatingGraphQLLinkDataFetcher`.

Resolution workflow (high-level):

1. Client requests a query against the gateway.
2. When the gateway executes a field that is marked as a linked field, the `RemoteDelegatingGraphQLLinkDataFetcher` is invoked.
3. The data fetcher extracts the source value (from `environment.getSource()`), then uses the `GraphQLFieldLinkDefinition` metadata to build remote GraphQL variables.
   - If the link has a single argument and the extracted value is not a Map, the value is assigned to that single remote argument.
   - If multiple arguments are declared, the extractor expects the source/method to return a Map-like object whose keys match the argument names (the builder will throw if a required argument is missing).
4. The gateway uses `QueryLinkBuilder` and related builders (`LinkOperationBuilder`, `SelectionSetBuilder`, etc.) to render a remote GraphQL query string and variable definitions that preserve requested selection sets and inline fragments.
5. The gateway executes the remote query via `RemoteGraphQLExecutionClient.execute(SchemaLocation, ExecutionInput)` and normalizes the returned data (scalar normalization, inline fragments preservation) before returning it to the original client.

Examples
--------

Example domain class (catalog) using both field and method link styles:

```
@Data
@GraphQLLinkClass
public class Catalog {

    private UUID id;
    private String name;
    private String description;

    @GraphQLLink(schema = "product", type = "Product", query = "productsByIds", arguments = {
            @GraphQLLinkArgument(name = "ids")
    }, collection = true)
    private List<UUID> productIds;

    @GraphQLQuery(name = "products")
    public List<UUID> getProductIds() {
        return productIds;
    }

    @GraphQLLink(schema = "product", type = "Page_Product", query = "products", arguments = {@GraphQLLinkArgument(name = "catalogId"), @GraphQLLinkArgument(name = "q")})
    @GraphQLQuery(name = "productsByNames")
    public GraphQLLinkArguments getProductNames(@GraphQLArgument(name = "name") String name) {
        return new GraphQLLinkArguments(Map.of("catalogId", id, "q", name));
    }
}
```

Single-argument resolution (field `productIds`): the gateway will produce a remote request like:

```
query ($ids: [ID!]) {
  productsByIds(ids: $ids) {
    ...requested selection...
  }
}
```

Multi-argument resolution (method `productsByNames`): the gateway will render a variables definition for each declared argument and populate them using the Map returned by the method. The selection set requested by the client is preserved and sent to the remote service.

Notes and error handling
------------------------

- If a linked field declares multiple arguments but the source value is not a Map (or lacks required keys) the gateway will throw an `IllegalStateException` during query construction.
- Inline fragments are preserved by the `SelectionSetBuilder` and `InLineFragmentBuilder` so polymorphic responses (e.g. `PhysicalProduct` / `DigitalProduct`) are correctly routed and normalized.
- The gateway validates that each target argument type is a `GraphQLInputType` and will throw if metadata is missing or inconsistent.

Tips for service authors
------------------------

- Use `@GraphQLLink` on fields when you can map the field value directly to a single remote argument (IDs, lists of IDs).
- Use `@GraphQLLink` on a method when you need to map multiple arguments — return a `Map` or `GraphQLLinkArguments` that maps target argument names to values.
- Always annotate the getter with `@GraphQLQuery(name = "xxx")` when building metadata from fields — the metadata builder uses the property descriptor's read method to find the GraphQL field name.

Related code pointers
---------------------

- `GraphQLLinkedTypeBuilder`, `GraphQLFieldLinkDefinitionBuilder` (metadata extraction)
- `GraphQLLinkTypesMap` (gateway-side link registry)
- `RemoteDelegatingGraphQLLinkDataFetcher` (runtime resolution)
- `QueryLinkBuilder`, `SelectionSetBuilder`, `NestedFieldBuilder` (query rendering)
- `RemoteGraphQLExecutionClient` (executing remote queries)

See also
--------

- `docs/graphql.md` — high level GraphQL support in the platform
- `parent/graphql-gateway/README.md` — gateway module details and runtime configuration

```json
// Example response (single-argument case)
{
  "data": {
    "catalog": {
      "id": "11cac832-3721-47f5-b87a-ca38fb52fe0e",
      "name": "Summer 2026",
      "description": "Catalog for Summer 2026",
      "status": "ACTIVE",
      "createdAt": "2026-03-20T13:31:43.476Z",
      "updatedAt": "2026-03-20T13:31:43.476Z",
      "products": [ /* product objects */ ]
    }
  }
}
```

Contributions and troubleshooting
--------------------------------

If you find mismatches between the declared `@GraphQLLink` metadata and the remote schema (for example, missing target argument types), check the service schema registration in the registry and run the gateway in debug mode to inspect the built `GraphQLLinkTypesMap`.



