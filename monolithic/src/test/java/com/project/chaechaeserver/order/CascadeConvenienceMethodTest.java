//package com.project.chaechaeserver.order;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.beans.factory.annotation.Autowired;
//import static org.assertj.core.api.Assertions.*;
//
//import com.project.chaechaeserver.domain.model.order.OrderCustomerEntity;
//import com.project.chaechaeserver.domain.model.order.OrderItemEntity;
//import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
//import com.project.chaechaeserver.domain.repository.order.OrderCustomerRepository;
//import com.project.chaechaeserver.domain.repository.order.OrderItemRepository;
//
//import java.util.List;
//
//@SpringBootTest
//@Transactional
//public class CascadeConvenienceMethodTest {
//
//    @Autowired
//    private OrderCustomerRepository orderCustomerRepository;
//
//    @Autowired
//    private OrderItemRepository orderItemRepository;
//
//    @Test
//    public void 편의메서드_없이_cascade_테스트() {
//        System.out.println("\n=== 편의 메서드 없이 Cascade 테스트 ===");
//
//        // given: 주문 아이템 생성
//        OrderItemEntity item1 = OrderItemEntity.builder()
//            .productId(1)
//            .productName("사과")
//            .quantity(3)
//            .price(3000)
//            .build();
//
//        OrderItemEntity item2 = OrderItemEntity.builder()
//            .productId(2)
//            .productName("배")
//            .quantity(2)
//            .price(2000)
//            .build();
//
//        // 주문 생성 (편의 메서드 사용 안함)
//        OrderCustomerEntity order = OrderCustomerEntity.builder()
//            .customerId(1L)
//            .totalQuantity(5)  // 수동 계산
//            .totalPrice(13000) // 수동 계산 (3000*3 + 2000*2)
//            .status(StatusType.REQUESTED)
//            .build();
//
//        System.out.println("저장 전 상태:");
//        System.out.println("- 주문 아이템 리스트 크기: " + order.getOrderItems().size());
//        System.out.println("- item1의 부모 참조: " + item1.getOrderCustomer());
//        System.out.println("- item2의 부모 참조: " + item2.getOrderCustomer());
//
//        // when: 양방향 매핑 수동 설정 (편의 메서드 없이)
//        order.getOrderItems().add(item1);
//        order.getOrderItems().add(item2);
//        item1.updateOrderItem(order);  // 수동으로 부모 참조 설정
//        item2.updateOrderItem(order);  // 수동으로 부모 참조 설정
//
//        System.out.println("\n수동 매핑 후 상태:");
//        System.out.println("- 주문 아이템 리스트 크기: " + order.getOrderItems().size());
//        System.out.println("- item1의 부모 참조: " + (item1.getOrderCustomer() != null ? "있음" : "없음"));
//        System.out.println("- item2의 부모 참조: " + (item2.getOrderCustomer() != null ? "있음" : "없음"));
//
//        // 저장 (cascade 테스트)
//        OrderCustomerEntity savedOrder = orderCustomerRepository.save(order);
//
//        // then: 결과 확인
//        System.out.println("\n저장 후 상태:");
//        System.out.println("- 저장된 주문 ID: " + savedOrder.getId());
//
//        List<OrderItemEntity> savedItems = orderItemRepository.findAll();
//        System.out.println("- DB에 저장된 OrderItem 개수: " + savedItems.size());
//
//        for (OrderItemEntity savedItem : savedItems) {
//            System.out.println("  * " + savedItem.getProductName() +
//                             " (ID: " + savedItem.getId() +
//                             ", 부모 주문 ID: " + savedItem.getOrderCustomer().getId() + ")");
//        }
//
//        // 검증
//        assertThat(savedOrder.getId()).isNotNull();
//        assertThat(savedItems).hasSize(2);
//        assertThat(savedItems.get(0).getOrderCustomer().getId()).isEqualTo(savedOrder.getId());
//        assertThat(savedItems.get(1).getOrderCustomer().getId()).isEqualTo(savedOrder.getId());
//    }
//
//    @Test
//    public void save_all_성능_테스트 () {
//        System.out.println("\n=== 편의 메서드 있을 때 Cascade 테스트 ===");
//
//        // given: 주문 아이템들 생성
//        OrderItemEntity item1 = OrderItemEntity.builder()
//            .productId(3)
//            .productName("포도")
//            .quantity(2)
//            .price(4000)
//            .build();
//
//        OrderItemEntity item2 = OrderItemEntity.builder()
//            .productId(4)
//            .productName("수박")
//            .quantity(1)
//            .price(5000)
//            .build();
//
//        System.out.println("저장 전 상태:");
//        System.out.println("- item1의 부모 참조: " + item1.getOrderCustomer());
//        System.out.println("- item2의 부모 참조: " + item2.getOrderCustomer());
//
//        // when: Entity의 정적 팩토리 메서드 사용 (편의 메서드 포함)
//        OrderCustomerEntity order = OrderCustomerEntity.createOrder(2L,
//            List.of(item1, item2));
//
//        System.out.println("\ncreateOrder() 호출 후 상태:");
//        System.out.println("- 주문 아이템 리스트 크기: " + order.getOrderItems().size());
//        System.out.println("- 총 수량: " + order.getTotalQuantity());
//        System.out.println("- 총 금액: " + order.getTotalPrice());
//        System.out.println("- item1의 부모 참조: " + (item1.getOrderCustomer() != null ? "있음" : "없음"));
//        System.out.println("- item2의 부모 참조: " + (item2.getOrderCustomer() != null ? "있음" : "없음"));
//
//        // 저장 (cascade 테스트)
//        OrderCustomerEntity savedOrder = orderCustomerRepository.save(order);
//
//        // then: 결과 확인
//        System.out.println("\n저장 후 상태:");
//        System.out.println("- 저장된 주문 ID: " + savedOrder.getId());
//
//        List<OrderItemEntity> savedItems = orderItemRepository.findAll();
//        System.out.println("- DB에 저장된 OrderItem 개수: " + savedItems.size());
//
//        for (OrderItemEntity savedItem : savedItems) {
//            System.out.println("  * " + savedItem.getProductName() +
//                " (ID: " + savedItem.getId() +
//                ", 부모 주문 ID: " + savedItem.getOrderCustomer().getId() + ")");
//        }
//
//        // 검증
//        assertThat(savedOrder.getId()).isNotNull();
//        assertThat(savedItems).hasSize(2);
//        assertThat(order.getTotalQuantity()).isEqualTo(3); // 2 + 1
//        assertThat(order.getTotalPrice()).isEqualTo(13000); // 4000*2 + 5000*1
//    }
//
//    @Test
//    public void 편의메서드_있을때_cascade_테스트() {
//        System.out.println("\n=== 편의 메서드 있을 때 Cascade 테스트 ===");
//
//        // given: 주문 아이템들 생성
//        OrderItemEntity item1 = OrderItemEntity.builder()
//            .productId(3)
//            .productName("포도")
//            .quantity(2)
//            .price(4000)
//            .build();
//
//        OrderItemEntity item2 = OrderItemEntity.builder()
//            .productId(4)
//            .productName("수박")
//            .quantity(1)
//            .price(5000)
//            .build();
//
//        System.out.println("저장 전 상태:");
//        System.out.println("- item1의 부모 참조: " + item1.getOrderCustomer());
//        System.out.println("- item2의 부모 참조: " + item2.getOrderCustomer());
//
//        // when: Entity의 정적 팩토리 메서드 사용 (편의 메서드 포함)
//        OrderCustomerEntity order = OrderCustomerEntity.createOrder(2L,
//            List.of(item1, item2));
//
//        System.out.println("\ncreateOrder() 호출 후 상태:");
//        System.out.println("- 주문 아이템 리스트 크기: " + order.getOrderItems().size());
//        System.out.println("- 총 수량: " + order.getTotalQuantity());
//        System.out.println("- 총 금액: " + order.getTotalPrice());
//        System.out.println("- item1의 부모 참조: " + (item1.getOrderCustomer() != null ? "있음" : "없음"));
//        System.out.println("- item2의 부모 참조: " + (item2.getOrderCustomer() != null ? "있음" : "없음"));
//
//        // 저장 (cascade 테스트)
//        OrderCustomerEntity savedOrder = orderCustomerRepository.save(order);
//
//        // then: 결과 확인
//        System.out.println("\n저장 후 상태:");
//        System.out.println("- 저장된 주문 ID: " + savedOrder.getId());
//
//        List<OrderItemEntity> savedItems = orderItemRepository.findAll();
//        System.out.println("- DB에 저장된 OrderItem 개수: " + savedItems.size());
//
//        for (OrderItemEntity savedItem : savedItems) {
//            System.out.println("  * " + savedItem.getProductName() +
//                             " (ID: " + savedItem.getId() +
//                             ", 부모 주문 ID: " + savedItem.getOrderCustomer().getId() + ")");
//        }
//
//        // 검증
//        assertThat(savedOrder.getId()).isNotNull();
//        assertThat(savedItems).hasSize(2);
//        assertThat(order.getTotalQuantity()).isEqualTo(3); // 2 + 1
//        assertThat(order.getTotalPrice()).isEqualTo(13000); // 4000*2 + 5000*1
//    }
//
//
//    @Test
//    public void 양방향_매핑_누락시_cascade_실패_테스트() {
//        System.out.println("\n=== 양방향 매핑 누락 시 Cascade 실패 테스트 ===");
//
//        // given: 주문 아이템 생성
//        OrderItemEntity orphanItem = OrderItemEntity.builder()
//            .productId(99)
//            .productName("고아 아이템")
//            .quantity(1)
//            .price(1000)
//            .build();
//
//        // 주문 생성
//        OrderCustomerEntity order = OrderCustomerEntity.builder()
//            .customerId(999L)
//            .totalQuantity(1)
//            .totalPrice(1000)
//            .status(StatusType.REQUESTED)
//            .build();
//
//        System.out.println("저장 전 상태:");
//        System.out.println("- 주문 아이템 리스트 크기: " + order.getOrderItems().size());
//        System.out.println("- orphanItem의 부모 참조: " + orphanItem.getOrderCustomer());
//
//        // when: 한쪽 방향만 설정 (양방향 매핑 누락!)
//        order.getOrderItems().add(orphanItem);
//        // orphanItem.updateOrderItem(order); // ← 이걸 안함!
//
//        System.out.println("\n한쪽 매핑만 설정 후:");
//        System.out.println("- 주문 아이템 리스트 크기: " + order.getOrderItems().size());
//        System.out.println("- orphanItem의 부모 참조: " + orphanItem.getOrderCustomer());
//
//        // 저장 시도
//        OrderCustomerEntity savedOrder = orderCustomerRepository.save(order);
//
//        // then: 결과 확인
//        System.out.println("\n저장 후 상태:");
//        System.out.println("- 저장된 주문 ID: " + savedOrder.getId());
//
//        List<OrderItemEntity> allItems = orderItemRepository.findAll();
//        System.out.println("- DB에 저장된 총 OrderItem 개수: " + allItems.size());
//
//        long orphanCount = allItems.stream()
//            .filter(item -> item.getOrderCustomer() == null)
//            .count();
//
//        System.out.println("- 부모 참조가 null인 고아 아이템 개수: " + orphanCount);
//
//        // 양방향 매핑 누락으로 인한 문제 확인
//        assertThat(savedOrder.getId()).isNotNull();
//        // orphanItem은 cascade 대상이 아니므로 저장 안됨 (부모 참조 없음)
//        assertThat(orphanItem.getOrderCustomer()).isNull();
//    }
//
//    @Test
//    public void 편의메서드_vs_수동매핑_성능_비교() {
//        System.out.println("\n=== 편의 메서드 vs 수동 매핑 성능 비교 ===");
//
//        // Case 1: 수동 매핑 (여러 단계)
//        long startTime = System.nanoTime();
//
//        OrderItemEntity item1 = OrderItemEntity.builder()
//            .productId(10)
//            .productName("수동 상품1")
//            .quantity(1)
//            .price(1000)
//            .build();
//
//        OrderCustomerEntity manualOrder = OrderCustomerEntity.builder()
//            .customerId(10L)
//            .totalQuantity(1)
//            .totalPrice(1000)
//            .status(StatusType.REQUESTED)
//            .build();
//
//        manualOrder.getOrderItems().add(item1);
//        item1.updateOrderItem(manualOrder);
//
//        long manualTime = System.nanoTime() - startTime;
//        System.out.println("수동 매핑 소요 시간: " + manualTime + " ns");
//
//        // Case 2: 편의 메서드 (한 번에)
//        startTime = System.nanoTime();
//
//        OrderItemEntity item2 = OrderItemEntity.builder()
//            .productId(11)
//            .productName("편의 상품1")
//            .quantity(1)
//            .price(1000)
//            .build();
//
//        OrderCustomerEntity convenienceOrder = OrderCustomerEntity.createOrder(11L,
//            List.of(item2));
//
//        long convenienceTime = System.nanoTime() - startTime;
//        System.out.println("편의 메서드 소요 시간: " + convenienceTime + " ns");
//
//        // 결과 비교
//        System.out.println("편의 메서드가 " +
//            (manualTime > convenienceTime ? "더 빠름" : "더 느림") +
//            " (차이: " + Math.abs(manualTime - convenienceTime) + " ns)");
//
//        // 두 방식 모두 정상 동작하는지 확인
//        assertThat(manualOrder.getOrderItems()).hasSize(1);
//        assertThat(convenienceOrder.getOrderItems()).hasSize(1);
//        assertThat(item1.getOrderCustomer()).isEqualTo(manualOrder);
//        assertThat(item2.getOrderCustomer()).isEqualTo(convenienceOrder);
//    }
//}