package kr.hhplus.be.server.user.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

import lombok.Data;

@Schema(description = "사용자 응답")
@Data
public class UserResponse { 

    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "사용자 번호", example = "1")
    private Long userId;

    @Schema(description = "사용자명", example = "user123")
    private String username;

    @Schema(description = "상태", example = "ACTIVE")
    private String status;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;

    public UserResponse() {}

    public UserResponse(Long id, Long userId, String username, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

} 