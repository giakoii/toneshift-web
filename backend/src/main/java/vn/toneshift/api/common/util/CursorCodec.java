package vn.toneshift.api.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import vn.toneshift.api.common.exception.ApiException;
import vn.toneshift.api.common.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Encodes and decodes opaque cursors (Base64URL-encoded JSON) for cursor-based pagination.
 */
@Component
public class CursorCodec {

    private final ObjectMapper objectMapper;

    /**
     * Creates a codec backed by the application's {@link ObjectMapper}.
     *
     * @param objectMapper the JSON mapper used to serialize cursor values
     */
    public CursorCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Encodes cursor values into an opaque URL-safe string.
     *
     * @param cursor the cursor values to encode
     * @return the encoded cursor
     * @throws IllegalStateException if the cursor values cannot be serialized
     */
    public String encode(Map<String, Object> cursor) {
        try {
            byte[] json = objectMapper.writeValueAsString(cursor).getBytes(StandardCharsets.UTF_8);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new IllegalStateException("Không thể encode cursor", e);
        }
    }

    /**
     * Decodes an opaque cursor back into its values.
     *
     * @param cursor the encoded cursor received from the client
     * @return the decoded cursor values
     * @throws ApiException with {@link ErrorCode#MALFORMED_REQUEST} if the cursor is invalid
     */
    public Map<String, Object> decode(String cursor) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(cursor);
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new ApiException(ErrorCode.MALFORMED_REQUEST, "Cursor không hợp lệ");
        }
    }
}
