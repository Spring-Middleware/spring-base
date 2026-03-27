package io.github.spring.middleware.graphql.gateway.fetcher.builder;

public abstract class CommonBuilder<T extends CommonBuilder<T>> {

    protected final StringBuilder builder = new StringBuilder();

    @SuppressWarnings("unchecked")
    public T append(String str) {
        builder.append(str);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T appendBuilder(CommonBuilder<?> otherBuilder) {
        builder.append(otherBuilder.build());
        return (T) this;
    }

    public String build() {
        return builder.toString();
    }
}
