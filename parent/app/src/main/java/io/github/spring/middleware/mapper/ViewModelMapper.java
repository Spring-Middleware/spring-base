package io.github.spring.middleware.mapper;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ViewModelMapper<R, VM, M> {

    @Autowired
    private ApplicationContext applicationContext;

    public M map(R request, Class<VM> clazzViewModel, M model) {

        try {

            BeanInfo requestInfo = Introspector.getBeanInfo(request.getClass());
            BeanInfo viewModelInfo = Introspector.getBeanInfo(clazzViewModel);
            BeanInfo modelInfo = Introspector.getBeanInfo(model.getClass());
            Arrays.stream(viewModelInfo.getMethodDescriptors())
                    .filter(methodDescriptor -> !methodDescriptor.getName().equals("getClass"))
                    .forEach(methodDescriptor -> {

                        try {
                            VM vm = Optional.ofNullable(getViewModel(clazzViewModel))
                                    .orElseThrow(() -> new Exception("Can't instantiate " + clazzViewModel));

                            if (isMethodDescriptorInModel(methodDescriptor, modelInfo)) {

                                Method invokeMethod = methodDescriptor.getMethod();
                                Parameter[] parameters = invokeMethod.getParameters();
                                Object result = null;
                                if (parameters.length > 0) {
                                    result = applyViewModelWithParameters(request, vm, invokeMethod, requestInfo,
                                            parameters);
                                } else {
                                    result = applyViewModelWithoutParameters(request, invokeMethod, requestInfo);
                                }

                                String propertyName = getPropertyName(methodDescriptor.getMethod(), "get");
                                Method writeMethod = Arrays.stream(modelInfo.getMethodDescriptors())
                                        .filter(md -> getMethodName(propertyName, "set").equals(md.getName()))
                                        .map(MethodDescriptor::getMethod).findFirst()
                                        .orElseThrow(() -> new Exception(
                                                "Missing setter method on " +
                                                        modelInfo.getBeanDescriptor().getDisplayName()));
                                writeMethod.invoke(model, result);
                            }

                        } catch (Exception ex) {
                            log.error("Can't process property " + methodDescriptor.getName());
                        }
                    });

        } catch (Exception ex) {
            log.error("Error", ex);
        }
        return model;
    }

    private Object applyViewModelWithParameters(R request, VM vm, Method invokeMethod, BeanInfo requestInfo,
            Parameter[] parameters) throws Exception {

        Collection<Method> requestReadMethods = getReadMethodsForProperty(requestInfo,
                parameters);
        if (requestReadMethods.size() != parameters.length) {
            throw new Exception(
                    "Can't invoke " + invokeMethod.getName() +
                            " missing getter in request");
        }
        List<?> arguments = getInvokeResultForMethods(requestReadMethods, request);
        if (arguments.size() != parameters.length) {
            throw new Exception(
                    "Can't invoke " + invokeMethod.getName() + " missing argument");
        }
        return invokeMethod.invoke(vm, arguments.toArray());
    }

    private Object applyViewModelWithoutParameters(R request, Method invokeMethod,
            BeanInfo requestInfo) throws Exception {

        return Arrays.stream(requestInfo.getMethodDescriptors())
                .filter(md -> invokeMethod.getName().equals(md.getName()))
                .map(md -> {
                    try {
                        return md.getMethod().invoke(request);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }).findFirst()
                .orElseThrow(() -> new Exception("Can't get result for " + invokeMethod.getName()));
    }

    private String getMethodName(String name, String prefix) {

        return prefix + StringUtils.capitalize(name);
    }

    public String getPropertyName(Method method, String prefix) {

        return StringUtils.uncapitalize(method.getName().replaceFirst(prefix, ""));
    }

    private boolean isMethodDescriptorInModel(MethodDescriptor methodDescriptor, BeanInfo modelInfo) {

        return Arrays.stream(modelInfo.getPropertyDescriptors()).map(PropertyDescriptor::getReadMethod)
                .anyMatch(m -> m.getName().equals(methodDescriptor.getName()));
    }

    private List<?> getInvokeResultForMethods(Collection<Method> requestReadMethods, R request) {

        return requestReadMethods.stream().map(m -> {
            try {
                return m.invoke(request);
            } catch (Exception ex) {
                log.error("Error invoking method " + m.getName() + " on " + request.getClass().getSimpleName());
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Collection<Method> getReadMethodsForProperty(BeanInfo requestInfo, Parameter[] parameters) {

        return Arrays.stream(parameters).map(parameter -> {
            try {
                return getReadMethodRequest(requestInfo, parameter);
            } catch (Exception e) {
                log.error("Error", e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Method getReadMethodRequest(BeanInfo requestInfo, Parameter parameter) throws Exception {

        return Arrays.stream(requestInfo.getPropertyDescriptors()).filter(p -> p.getName().equals(parameter.getName()))
                .map(p -> p.getReadMethod()).findFirst().orElseThrow(() -> new Exception(
                        "No found getter for " + parameter.getName() + " in " +
                                requestInfo.getBeanDescriptor().getDisplayName()));

    }

    private VM getViewModel(Class<VM> vmClass) {

        VM vm = null;
        try {
            vm = applicationContext.getBean(vmClass);
        } catch (Exception ex) {
            try {
                vm = vmClass.newInstance();
            } catch (Exception ex2) {
                log.error("Can't instantiate clazz " + vmClass.getName(), ex2);
            }
        }
        return vm;
    }

}
