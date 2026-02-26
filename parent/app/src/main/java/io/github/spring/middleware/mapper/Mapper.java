package io.github.spring.middleware.mapper;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.util.Collection;
import java.util.stream.Collectors;

public abstract class Mapper<S, D> {

    private ModelMapper modelMapper = new ModelMapper();

    public Mapper() {

        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        configure(modelMapper);
    }

    public D map(S sourceElement, D targetElement) {

        modelMapper.map(sourceElement, targetElement);
        mapSpecific(sourceElement, targetElement);
        return targetElement;
    }

    public D map(S sourceElement, Class<D> clazzTarget) {

        try {
            D target = clazzTarget.newInstance();
            modelMapper.map(sourceElement, target);
            mapSpecific(sourceElement, target);
            return target;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public Collection<D> map(Collection<S> sourceList, Class<D> clazzTarget) {

        return sourceList.stream().map(s -> {
            try {
                D target = clazzTarget.newInstance();
                modelMapper.map(s, target);
                mapSpecific(s, target);
                return target;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList());
    }

    protected abstract void configure(ModelMapper modelMapper);

    protected abstract void mapSpecific(S s, D d);

}
