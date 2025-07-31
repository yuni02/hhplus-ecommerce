package kr.hhplus.be.server.user.adapter.in.dto;   

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "에러 응답")
@Data
public class ErrorResponse {        

    @Schema(description = "에러 메시지", example = "사용자 조회에 실패했습니다.")
    private String message;

    public ErrorResponse() {}

    public ErrorResponse(String message) {
        this.message = message;
    }

} 