package io.github.spring.middleware.converter;

public class MixInDefinition {

    private Class<?> targetClazz;
    private Class<?> mixInClazz;

    public MixInDefinition() {

    }

    public MixInDefinition(Class<?> targetClazz, Class<?> mixInClazz) {

        this.targetClazz = targetClazz;
        this.mixInClazz = mixInClazz;
    }

    public Class<?> getTargetClazz() {

        return targetClazz;
    }

    public void setTargetClazz(Class<?> targetClazz) {

        this.targetClazz = targetClazz;
    }

    public Class<?> getMixInClazz() {

        return mixInClazz;
    }

    public void setMixInClazz(Class<?> mixInClazz) {

        this.mixInClazz = mixInClazz;
    }
}
