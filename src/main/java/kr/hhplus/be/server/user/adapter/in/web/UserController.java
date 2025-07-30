package kr.hhplus.be.server.user.adapter.in.web;

import kr.hhplus.be.server.user.application.port.in.GetUserUseCase;
import kr.hhplus.be.server.user.application.response.ErrorResponse;
import kr.hhplus.be.server.user.application.response.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "사용자 관리 API")
public class UserController {

    private final GetUserUseCase getUserUseCase;

    public UserController(GetUserUseCase getUserUseCase) {
        this.getUserUseCase = getUserUseCase;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "사용자 조회", description = "특정 사용자의 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getUser(
            @Parameter(description = "사용자 ID", required = true, example = "1") 
            @PathVariable Long userId) {
        try {
            GetUserUseCase.GetUserCommand command = new GetUserUseCase.GetUserCommand(userId);
            var userOpt = getUserUseCase.getUser(command);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            GetUserUseCase.GetUserResult result = userOpt.get();
            UserResponse response = new UserResponse(
                    result.getId(),
                    result.getUserId(),
                    result.getUsername(),
                    result.getAmount(),
                    result.getStatus(),
                    result.getCreatedAt(),
                    result.getUpdatedAt()
            );
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("사용자 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
} 