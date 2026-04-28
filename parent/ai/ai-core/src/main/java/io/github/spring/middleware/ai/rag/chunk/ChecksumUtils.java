package io.github.spring.middleware.ai.rag.chunk;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

public final class ChecksumUtils {

    public static String checksum(String content, Map<String, String> metadata) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // contenido
            digest.update(content.getBytes(StandardCharsets.UTF_8));

            // metadata ordenada (importante para consistencia)
            metadata.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
                        digest.update(entry.getValue().getBytes(StandardCharsets.UTF_8));
                    });

            byte[] hash = digest.digest();
            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

}
