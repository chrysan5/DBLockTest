package com.example.locking;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

//비관적 락 테스트
@SpringBootTest
public class ItemServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(ItemServiceTest.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    public void testPessimisticLocking() throws InterruptedException {
        // 초기 데이터 설정
        logger.info("초기 아이템 데이터를 설정합니다.");
        Item item = new Item();
        item.setName("Item 1");
        item.setQuantity(10);
        itemRepository.save(item);

        //동시에 요청하는 케이스를 만들기 위해 스레드를 2개 만들어 임의로 발생시킨다
        
        // 첫 번째 트랜잭션: 아이템 수량을 20으로 업데이트
        Thread thread1 = new Thread(() -> {
            logger.info("스레드 1: 아이템 수량 업데이트를 시도합니다.");
            itemService.updateItemQuantity(item.getId(), 20);
            logger.info("스레드 1: 아이템 수량 업데이트 완료.");
        });

        // 두 번째 트랜잭션: 아이템 수량을 30으로 업데이트
        Thread thread2 = new Thread(() -> {
            logger.info("스레드 2: 아이템 수량 업데이트를 시도합니다.");
            itemService.updateItemQuantity(item.getId(), 30);
            logger.info("스레드 2: 아이템 수량 업데이트 완료.");
        });

        // 두 스레드를 동시에 실행 (글자상 순서는 있지만 동시에 실행되는 것과 같다)
        thread2.start();
        thread1.start();


        // 두 스레드가 종료될 때까지 대기
        thread1.join();
        thread2.join();

        // 최종 결과를 확인합니다.
        Item updatedItem = itemService.findItemById(item.getId());
        logger.info("최종 아이템 수량: {}", updatedItem.getQuantity());
        //락은 적용되었지만 결과 20으로 업데이트 되어있다. -> 30으로 업데이트 된 사항을 놓칠 수 있음
        //-> 모든 이벤트를 기록하는 이벤트 소싱을 해야한다.
    }
}

/*select
        i1_0.id,
        i1_0.name,
        i1_0.quantity
    from
        item i1_0
    where
        i1_0.id=? for update
 여기서 for update는 락이다 -> 락어노테이션이 정상 동작함을 알 수 있다 */