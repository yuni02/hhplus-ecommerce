package kr.hhplus.be.server.order.domain;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    
    Order save(Order order);
    
    Optional<Order> findById(Long id);
    
    List<Order> findByUserId(Long userId);
    
    boolean existsById(Long id);
} 