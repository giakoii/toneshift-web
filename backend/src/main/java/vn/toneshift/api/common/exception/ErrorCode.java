package vn.toneshift.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Single source of truth mapping each API error code to its HTTP status and default message.
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không đúng định dạng"),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Thiếu hoặc token không hợp lệ"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã hết hạn"),

    FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
    ACCOUNT_BLOCKED(HttpStatus.FORBIDDEN, "Tài khoản đã bị khoá"),

    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên"),
    SONG_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy bài hát"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy bình luận"),
    SETLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy setlist"),
    SHARE_NOT_FOUND(HttpStatus.NOT_FOUND, "Liên kết chia sẻ không tồn tại"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"),

    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email đã được sử dụng"),
    ALREADY_REPORTED(HttpStatus.CONFLICT, "Bạn đã báo cáo bình luận này"),
    SONG_ALREADY_IN_SETLIST(HttpStatus.CONFLICT, "Bài hát đã có trong setlist"),
    VERSION_CONFLICT(HttpStatus.CONFLICT, "Dữ liệu đã bị thay đổi bởi người khác, vui lòng tải lại"),
    CONFLICT(HttpStatus.CONFLICT, "Yêu cầu xung đột với dữ liệu hiện có"),

    COMMENT_REPLY_DEPTH_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "Chỉ được trả lời bình luận gốc"),
    COMMENT_DELETED(HttpStatus.UNPROCESSABLE_ENTITY, "Bình luận đã bị xoá"),
    SONG_NOT_PUBLIC(HttpStatus.UNPROCESSABLE_ENTITY, "Bài hát chưa được công khai"),
    CANNOT_RATE_OWN_SONG(HttpStatus.UNPROCESSABLE_ENTITY, "Không thể tự đánh giá bài hát của mình"),
    SETLIST_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "Đã vượt quá giới hạn cho phép"),
    REORDER_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "Danh sách sắp xếp lại không khớp với dữ liệu hiện tại"),
    SONG_UNAVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Bài hát hiện không khả dụng"),
    RESET_TOKEN_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"),
    FORBIDDEN_OPERATION(HttpStatus.UNPROCESSABLE_ENTITY, "Thao tác không được phép"),

    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Bạn đã gửi quá nhiều yêu cầu, vui lòng thử lại sau"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã có lỗi xảy ra, vui lòng thử lại sau");

    private final HttpStatus status;
    private final String defaultMessage;

    // Binds the error code to its HTTP status and default client-facing message.
    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    /**
     * Returns the HTTP status associated with this error code.
     *
     * @return the HTTP status
     */
    public HttpStatus status() {
        return status;
    }

    /**
     * Returns the default client-facing message of this error code.
     *
     * @return the default message
     */
    public String defaultMessage() {
        return defaultMessage;
    }
}
