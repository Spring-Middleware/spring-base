package io.github.spring.middleware.error;

public interface ErrorCodes {

    String getCode();

    default String getMessage() {
        return "An error occurred";
    }

    static ErrorCodes fromCode(String code, String message) {
        return new ErrorCodes(){
            @Override
            public String getCode() {
                return code;
            }

            @Override
            public String getMessage() {
                return message;
            }
        };
    }
}
