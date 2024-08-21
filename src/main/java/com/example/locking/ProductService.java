package com.example.locking;

import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public void updateProductPrice(Long productId, Double newPrice) {
        try {
            Product product = productRepository.findById(productId) // 기존 데이터를 읽어옵니다.
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            product.setPrice(newPrice); // 가격을 수정합니다.
            productRepository.save(product); // 저장 시 버전 충돌이 발생하면 예외가 발생합니다.
        } catch (ObjectOptimisticLockingFailureException e) { // 낙관적 락 예외 처리
            System.err.println("낙관적 락 충돌이 발생했습니다. 다른 트랜잭션이 먼저 데이터를 수정했습니다.");
            throw e;
        }
    }
}
