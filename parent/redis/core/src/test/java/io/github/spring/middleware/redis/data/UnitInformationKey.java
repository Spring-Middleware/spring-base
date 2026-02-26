package io.github.spring.middleware.redis.data;

import io.github.spring.middleware.redis.unit.RedisUnitKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UnitInformationKey implements RedisUnitKey {

    private String externalSystem;
    private String code;

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        UnitInformationKey that = (UnitInformationKey) o;

        return new EqualsBuilder().append(externalSystem, that.externalSystem)
                .append(code, that.code).isEquals();
    }

    @Override
    public int hashCode() {

        return new HashCodeBuilder(17, 37).append(externalSystem).append(code).toHashCode();
    }
}
