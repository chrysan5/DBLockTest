package com.example.locking;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertThrows;

//낙관적 락 테스트
@SpringBootTest
public class ProductServiceTest {
    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    public void testOptimisticLocking() throws InterruptedException {
        // 초기 데이터 설정
        Product product = new Product();
        product.setName("Product 1");
        product.setPrice(100.0);
        productRepository.save(product);

        // 첫 번째 트랜잭션: 상품 가격을 200.0으로 업데이트
        Thread thread1 = new Thread(() -> {
            productService.updateProductPrice(product.getId(), 200.0);
        });

        // 두 번째 트랜잭션: 상품 가격을 300.0으로 업데이트
        Thread thread2 = new Thread(() -> {
            //assertThrows : 스레드2에서 이 예외가 발생할 것으로 예상하는데, 가격을 300으로 업데이트시 이 예외 발생할 것이다.
            assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
                productService.updateProductPrice(product.getId(), 300.0);
            });
        });

        // 두 스레드를 동시에 실행
        thread1.start();
        thread2.start();

        // 두 스레드가 종료될 때까지 대기
        thread1.join();
        thread2.join();
    }
}

/*스레드1번의 버전이 올라가서 스레드2번이 버전이 변경됨을 확인하여 롤백된다.
=> 둘다 버전값이 0인애를 select후 이후 update하는데 스레드1이 먼저해서 버전을 1로 바꿨으므로 스레드2는 버전차이가 생기는 것이다.
 Thread-4] binding parameter (1:VARCHAR) <- [Product 1]
 Thread-4]  binding parameter (2:DOUBLE) <- [200.0]
 Thread-4]binding parameter (3:INTEGER) <- [1]
 Thread-4]  binding parameter (4:BIGINT) <- [1]
 Thread-4]  binding parameter (5:INTEGER) <- [0]

 Thread-5]  binding parameter (1:VARCHAR) <- [Product 1]
 Thread-5]  binding parameter (2:DOUBLE) <- [300.0]
 Thread-5]  binding parameter (3:INTEGER) <- [1]
 Thread-5]  binding parameter (4:BIGINT) <- [1]
 Thread-5]  binding parameter (5:INTEGER) <- [0] */