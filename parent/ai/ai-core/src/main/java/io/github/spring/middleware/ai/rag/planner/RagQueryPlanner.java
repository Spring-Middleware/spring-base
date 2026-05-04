package io.github.spring.middleware.ai.rag.planner;

import reactor.core.publisher.Mono;

public interface RagQueryPlanner {
    Mono<RagQueryPlan> plan(SelfQueryRequest request);
}

