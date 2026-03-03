package io.github.spring.middleware.constraint.deflt;

import io.github.spring.middleware.constraint.DbConstraintMapper;
import io.github.spring.middleware.error.ErrorDescriptor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class DefaultDbConstraintMapper implements DbConstraintMapper {

    private final Map<String, ErrorDescriptor> errorDescriptorMap = new HashMap<>();

    @Override
    public Optional<ErrorDescriptor> mapConstraintName(String constraintName) {
        return Optional.ofNullable(errorDescriptorMap.get(norm(constraintName)));
    }

    private String norm(String s) {
        return s == null ? "" : s.toUpperCase();
    }

    public void putCode(String name, ErrorDescriptor errorDescriptor) {
        errorDescriptorMap.put(norm(name), errorDescriptor);
    }

    public void putMessage(String name, ErrorDescriptor errorDescriptor) {
        errorDescriptorMap.put(norm(name), errorDescriptor);
    }


}
