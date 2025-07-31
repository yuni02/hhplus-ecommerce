package kr.hhplus.be.server.shared.api;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * 공통 API 문서화 인터페이스
 * 모든 API에서 공통으로 사용하는 응답 코드 정의
 */
public interface ApiDocumentation {

    /**
     * 공통 성공 응답
     */
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    interface SuccessResponse {}

    /**
     * 공통 에러 응답
     */
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    interface ErrorResponse {}

    /**
     * 공통 CRUD 응답
     */
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    interface CrudResponse {}
} 