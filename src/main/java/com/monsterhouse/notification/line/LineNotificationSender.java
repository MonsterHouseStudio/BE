package com.monsterhouse.notification.line;

import com.monsterhouse.common.config.LineProperties;
import com.monsterhouse.notification.line.dto.LinePushRequest;
import com.monsterhouse.notification.sender.NotificationChannel;
import com.monsterhouse.notification.sender.NotificationMessage;
import com.monsterhouse.notification.sender.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 기획서 §5 — LINE Messaging API 관리자 알림.
 *
 * ⚠ LINE Notify 는 2025-03-31 종료되었습니다. 인터넷 튜토리얼 상당수가 그 방식이니 주의.
 *
 * 설계 원칙 (§5.3):
 *  - 고객→관리자 단방향. 고객에게 LINE 자동 답장은 하지 않습니다.
 *    (친구추가 전제 + "수신자 수 × 메시지 수" 차감으로 무료 200통이 순식간에 소진)
 *  - 발송 실패가 예약/문의 자체를 실패시키면 안 됩니다. 여기서 던진 예외는
 *    NotificationService 가 잡아 로깅만 합니다.
 */
@Slf4j
@Component
public class LineNotificationSender implements NotificationSender {

    private final RestClient restClient;
    private final LineProperties properties;

    public LineNotificationSender(@Qualifier("lineRestClient") RestClient restClient,
                                  LineProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.LINE;
    }

    @Override
    public boolean isEnabled() {
        return properties.enabled()
                && hasText(properties.channelAccessToken())
                && hasText(properties.adminUserId());
    }

    @Override
    public void send(NotificationMessage message) {
        String to = hasText(message.to()) ? message.to() : properties.adminUserId();
        LinePushRequest payload = LinePushRequest.text(to, message.body());

        RuntimeException lastError = null;

        for (int attempt = 1; attempt <= properties.maxRetry(); attempt++) {
            try {
                restClient.post()
                        .uri(properties.pushUrl())
                        .header("Authorization", "Bearer " + properties.channelAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            // 4xx 는 재시도해도 같은 결과 + 통수만 낭비 → 즉시 중단
                            throw new LinePermanentException(
                                    "LINE push rejected. status=" + res.getStatusCode());
                        })
                        .toBodilessEntity();

                log.info("LINE push sent. attempt={}", attempt);
                return;

            } catch (LinePermanentException e) {
                log.error("LINE push permanently failed. 재시도하지 않습니다.", e);
                throw e;

            } catch (RestClientException e) {
                lastError = e;
                log.warn("LINE push failed. attempt={}/{}", attempt, properties.maxRetry(), e);
                sleepBackoff(attempt);
            }
        }

        throw new IllegalStateException("LINE push failed after retries", lastError);
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(500L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** 토큰 만료·잘못된 userId 등 재시도가 무의미한 실패 */
    static class LinePermanentException extends RuntimeException {
        LinePermanentException(String message) {
            super(message);
        }
    }
}