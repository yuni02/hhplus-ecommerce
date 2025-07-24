package kr.hhplus.be.server.coupon.application.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 응답")
public class ErrorResponse {

    @Schema(description = "에러 메시지", example = "쿠폰 발급에 실패했습니다.")
    private String message;

    public ErrorResponse() {}

    public ErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
} 