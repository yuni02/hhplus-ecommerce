package kr.hhplus.be.server.product.domain;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    
    List<Product> findAllActive();
    
    Optional<Product> findById(Long id);
    
    Product save(Product product);
    
    boolean existsById(Long id);
}