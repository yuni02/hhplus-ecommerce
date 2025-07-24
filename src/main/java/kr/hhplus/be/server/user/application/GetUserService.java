package kr.hhplus.be.server.user.application;

import kr.hhplus.be.server.user.application.port.in.GetUserUseCase;
import kr.hhplus.be.server.user.application.port.out.LoadUserPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 사용자 조회 Application 서비스
 */
@Service
public class GetUserService implements GetUserUseCase {

    private final LoadUserPort loadUserPort;

    public GetUserService(LoadUserPort loadUserPort) {
        this.loadUserPort = loadUserPort;
    }

    @Override
    public Optional<GetUserResult> getUser(GetUserCommand command) {
        return loadUserPort.loadUserById(command.getUserId())
                .map(userInfo -> new GetUserResult(
                        userInfo.getId(),
                        userInfo.getName(),
                        userInfo.getEmail(),
                        userInfo.getPhoneNumber(),
                        userInfo.getStatus(),
                        null, // createdAt은 별도 조회 필요
                        null  // updatedAt은 별도 조회 필요
                ));
    }
} 