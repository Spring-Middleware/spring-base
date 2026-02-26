package io.github.spring.middleware.component;

import io.github.spring.middleware.error.ErrorMessage;
import io.github.spring.middleware.exception.ServiceException;
import io.github.spring.middleware.request.BaseRequest;
import io.github.spring.middleware.response.BaseResponse;
import io.github.spring.middleware.response.CollectionBaseResponse;
import io.github.spring.middleware.response.SingleBaseResponse;
import io.github.spring.middleware.view.View;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.function.Supplier;

@Component
public class ResponseFactory {

    @Autowired
    private DateTimeAndServer dateTimeAndServer;

    public <V extends View, R extends BaseRequest, C extends SingleBaseResponse<V>> C createSingleResponse(
            Class<C> clazz, V data,
            R request, Collection<ErrorMessage> errors) throws ServiceException {

        try {
            C response = clazz.getDeclaredConstructor().newInstance();
            completeResponse(response, request::getRequestUUid, errors);
            response.setData(data);
            return response;
        } catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public <V extends View, R extends BaseRequest, C extends CollectionBaseResponse> C createCollectionResponse(
            Class<C> clazz,
            Collection<V> data,
            R request, Collection<ErrorMessage> errors) throws ServiceException {

        try {
            C response = clazz.getDeclaredConstructor().newInstance();
            completeResponse(response, request::getRequestUUid, errors);
            response.setData(data);
            return response;
        } catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public <C extends CollectionBaseResponse> C createCollectionErrorResponse(
            Class<C> clazz, String requestId, Collection<ErrorMessage> errors) throws ServiceException {

        try {
            C response = clazz.getDeclaredConstructor().newInstance();
            completeResponse(response, () -> requestId, errors);
            return response;
        } catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    private <B extends BaseResponse, R extends BaseRequest> B completeResponse(B baseReponse,
            Supplier<String> requestIdSupplier,
            Collection<ErrorMessage> errors) throws Exception {

        baseReponse.setResponseId(requestIdSupplier.get());
        baseReponse.setDateTime(dateTimeAndServer.getCurrentTime());
        baseReponse.setErrors(errors);
        baseReponse.setServer(dateTimeAndServer.getServerName());
        return baseReponse;
    }
}
