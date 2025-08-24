package kr.hhplus.be.server.coupon.adapter.in.dto;

/**
 * 쿠폰 대기열 응답 DTO
 */
public class CouponQueueResponse {
    private String message;
    private Long queuePosition;
    private Long queueSize;

    public CouponQueueResponse() {
    }

    public CouponQueueResponse(String message, Long queuePosition, Long queueSize) {
        this.message = message;
        this.queuePosition = queuePosition;
        this.queueSize = queueSize;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(Long queuePosition) {
        this.queuePosition = queuePosition;
    }

    public Long getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(Long queueSize) {
        this.queueSize = queueSize;
    }
}
