package kr.hhplus.be.server.product.infrastructure.scheduler;

import kr.hhplus.be.server.product.domain.service.GetPopularProductsService;
import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularProductCacheScheduler {

            private final GetPopularProductsService getPopularProductsService;

            @Scheduled(cron = "0 0 */6 * * *")  // 1시간마다 실행
            public void refreshPopularProductsCache() {
                try {
                    log.info("인기 상품 캐시 갱신 시작");
                    GetPopularProductsUseCase.GetPopularProductsCommand command = new GetPopularProductsUseCase.GetPopularProductsCommand(5);
                    getPopularProductsService.getPopularProducts(command);
                    log.info("인기 상품 캐시 갱신 완료");
                } catch (Exception e) {
                    log.error("인기 상품 캐시 갱신 중 오류 발생", e);
                }
            }
        }