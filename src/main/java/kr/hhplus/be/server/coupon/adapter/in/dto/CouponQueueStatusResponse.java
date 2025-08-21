package kr.hhplus.be.server.coupon.adapter.in.dto;

/**
 * 쿠폰 대기열 상태 응답 DTO
 */
public class CouponQueueStatusResponse {
    private String status; // PROCESSING, SUCCESS, FAILED
    private String message;
    private Long queuePosition;
    private Long queueSize;

    public CouponQueueStatusResponse() {
    }

    public CouponQueueStatusResponse(String status, String message, Long queuePosition, Long queueSize) {
        this.status = status;
        this.message = message;
        this.queuePosition = queuePosition;
        this.queueSize = queueSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
