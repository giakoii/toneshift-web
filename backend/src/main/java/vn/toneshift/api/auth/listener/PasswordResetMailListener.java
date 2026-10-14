package vn.toneshift.api.auth.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.toneshift.api.auth.event.PasswordResetRequestedEvent;
import vn.toneshift.api.common.logging.AppLog;
import vn.toneshift.api.common.logging.LogMasker;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Emails the password reset link.
 *
 * <p>Runs after commit so the token is already stored, and off the request thread so a slow or
 * unreachable mail server cannot hold up the response, which is deliberately the same whether or
 * not the account exists.
 */
@Component
@RequiredArgsConstructor
public class PasswordResetMailListener {

    private static final AppLog log = AppLog.get(PasswordResetMailListener.class);

    private final JavaMailSender mailSender;

    @Value("${app.public-web-url}")
    private String publicWebUrl;

    @Value("${app.mail.from}")
    private String from;

    /**
     * Sends the reset link to the requesting address.
     *
     * @param event the issued reset token
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(event.email());
        message.setSubject("Đặt lại mật khẩu ToneShift");
        message.setText(body(event));
        mailSender.send(message);
        // The address is masked and the token never appears.
        log.info("Đã gửi mail đặt lại mật khẩu tới {}", LogMasker.maskEmail(event.email()));
    }

    // Builds the message body, with the token in the link only.
    private String body(PasswordResetRequestedEvent event) {
        String link = publicWebUrl + "/reset-password?token="
                + URLEncoder.encode(event.rawToken(), StandardCharsets.UTF_8);
        long minutes = Duration.between(Instant.now(), event.expiresAt()).toMinutes();
        return """
                Xin chào,

                Bạn (hoặc ai đó) đã yêu cầu đặt lại mật khẩu cho tài khoản ToneShift này.
                Mở liên kết sau để đặt mật khẩu mới, liên kết có hiệu lực trong %d phút và chỉ dùng được một lần:

                %s

                Nếu bạn không yêu cầu, hãy bỏ qua email này, mật khẩu hiện tại vẫn giữ nguyên.

                ToneShift
                """.formatted(Math.max(minutes, 1), link);
    }
}
