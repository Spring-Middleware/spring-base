package com.middleware.sort;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort.Direction;

import java.util.Collection;

@Getter
@Setter
@Builder
@NoArgsConstructor
public class SortCriteria {

    @NotEmpty(message = "Missing at least one property")
    private Collection<String> properties;
    @NotNull(message = "Missing direction")
    private Direction direction;

    public SortCriteria(
            @NotEmpty(message = "Missing at least one property") Collection<String> properties,
            @NotNull(message = "Missing direction") Direction direction) {

        this.properties = properties;
        this.direction = direction;
    }

    public Collection<String> getProperties() {

        return properties;
    }

    public void setProperties(Collection<String> properties) {

        this.properties = properties;
    }

    public Direction getDirection() {

        return direction;
    }

    public void setDirection(Direction direction) {

        this.direction = direction;
    }
}
