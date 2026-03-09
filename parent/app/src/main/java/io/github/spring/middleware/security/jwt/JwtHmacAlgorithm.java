package io.github.spring.middleware.security.jwt;

public enum JwtHmacAlgorithm {

    HS256("HmacSHA256"),
    HS384("HmacSHA384"),
    HS512("HmacSHA512");

    private final String javaName;

    JwtHmacAlgorithm(String javaName) {
        this.javaName = javaName;
    }

    public String javaName() {
        return javaName;
    }
}
