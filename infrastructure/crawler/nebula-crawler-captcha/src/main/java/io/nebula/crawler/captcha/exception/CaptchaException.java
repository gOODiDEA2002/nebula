package io.nebula.crawler.captcha.exception;

/**
 * 验证码异常
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public class CaptchaException extends Exception {

    public CaptchaException(String message) {
        super(message);
    }

    public CaptchaException(String message, Throwable cause) {
        super(message, cause);
    }

    public CaptchaException(Throwable cause) {
        super(cause);
    }
}
