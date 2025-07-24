package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.facade.OrderFacade;

import org.springframework.stereotype.Service;

/**
 * 주문 생성 Application 서비스 (Facade 패턴 적용)
 */
@Service
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderFacade orderFacade;

    public CreateOrderService(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
    }

    @Override
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        return orderFacade.createOrder(command);
    }
} 