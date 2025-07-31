package kr.hhplus.be.server.shared.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "에러 응답")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    @Schema(description = "에러 메시지", example = "요청 처리에 실패했습니다.")
    private String message;

    @Schema(description = "에러 코드", example = "ERROR_001")
    private String code;

    @Schema(description = "에러 발생 시간", example = "2024-01-01T12:00:00")
    private String timestamp;

    public ErrorResponse(String message) {
        this.message = message;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

    public ErrorResponse(String message, String code) {
        this.message = message;
        this.code = code;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message);
    }

    public static ErrorResponse of(String message, String code) {
        return new ErrorResponse(message, code);
    }
} 