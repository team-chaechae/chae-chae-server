package com.project.chaechaeserver.domain.model.inventory;

import com.project.chaechaeserver.domain.model.inventory.constraint.InventoryChangeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 재고 변경 이력을 저장하는 엔티티
 *
 * <p>이 엔티티는 재고 변경의 모든 내역을 기록하여 추적성(Traceability)과
 * 감사(Audit) 기능을 제공합니다. Write-Back 패턴에서 Redis 장애 시 복구 수단으로도 활용됩니다.</p>
 *
 * <h3>주요 특징</h3>
 * <ul>
 *   <li>모든 재고 변경을 불변 기록으로 저장 (Insert-Only, No Update/Delete)</li>
 *   <li>변경 전후 수량을 함께 저장하여 정확한 이력 추적</li>
 *   <li>변경 타입별 팩토리 메서드로 일관된 데이터 생성</li>
 *   <li>주문 ID 연계로 주문과 재고의 완전한 추적 가능</li>
 * </ul>
 *
 * <h3>사용 목적</h3>
 * <ol>
 *   <li><b>감사 및 추적:</b> 누가, 언제, 왜 재고를 변경했는지 완전히 파악</li>
 *   <li><b>장애 복구:</b> Redis 장애 시 히스토리에서 최신 재고 상태 복원</li>
 *   <li><b>분석:</b> 재고 이동 패턴, 판매 트렌드, 재고 회전율 분석</li>
 *   <li><b>규정 준수:</b> 재무 감사나 규제 요구사항 충족</li>
 * </ol>
 *
 * @see InventoryChangeType
 */
@Table(name = "inventory_history")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "change_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private InventoryChangeType changeType;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "reason")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public InventoryHistoryEntity(Long productId, InventoryChangeType changeType,
        Integer quantityChange, Integer quantityBefore, Integer quantityAfter,
        Long orderId, String reason) {
        this.productId = productId;
        this.changeType = changeType;
        this.quantityChange = quantityChange;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.orderId = orderId;
        this.reason = reason;
    }

    /**
     * 주문 생성으로 인한 재고 차감 이력을 기록합니다.
     *
     * <p><b>기능 설명:</b></p>
     * <p>고객이 주문을 생성할 때 Redis에서 재고가 차감되는 시점에 이 히스토리를 DB에 즉시 기록합니다.
     * ORDER_DECREASE 타입으로 저장되며, 주문 ID와 연결되어 완전한 추적이 가능합니다.</p>
     *
     * <p><b>왜 이 메서드가 필요한가:</b></p>
     * <p>Write-Back 패턴에서 재고 차감은 Redis에만 먼저 반영되고 DB 동기화는 나중에 이루어집니다.
     * 만약 Redis가 장애로 중단되면 최근 5초간의 재고 변경이 손실될 수 있습니다.
     * 하지만 이 히스토리는 재고 차감과 동시에 DB에 기록되므로, Redis 장애 시 히스토리에서
     * 모든 주문의 재고 차감 내역을 복원할 수 있습니다.
     *
     * 또한 이 히스토리를 통해:
     * - 어떤 주문이 어떤 상품의 재고를 차감했는지 정확히 추적
     * - 재고 부족 분쟁 발생 시 증거 자료로 활용
     * - 일별/월별 판매량 분석에 활용</p>
     *
     * <p><b>왜 이렇게 동작하는가:</b></p>
     * <ol>
     *   <li><b>변경 타입:</b> ORDER_DECREASE로 설정하여 주문에 의한 차감임을 명시</li>
     *   <li><b>변경 전후 수량:</b> quantityBefore와 quantityAfter를 모두 저장하여
     *       재고 변화를 정확히 파악 가능</li>
     *   <li><b>변경 수량:</b> quantityChange에 차감된 수량을 저장 (양수로 저장)</li>
     *   <li><b>주문 ID:</b> 어떤 주문이 이 재고 변경을 일으켰는지 추적 가능</li>
     *   <li><b>이유:</b> "주문으로 인한 재고 차감"이라는 명확한 사유 기록</li>
     *   <li><b>타임스탬프:</b> @CreationTimestamp로 정확한 변경 시각 자동 기록</li>
     * </ol>
     *
     * <p><b>사용 예시:</b></p>
     * <pre>
     * // RedisInventoryService.decreaseStock()에서 호출
     * Integer currentStock = 100;  // Redis에서 조회한 현재 재고
     * Long newStock = 95L;         // Redis DECR 후 새 재고
     * Integer quantity = 5;        // 주문 수량
     * Long orderId = 12345L;       // 주문 ID
     *
     * InventoryHistoryEntity history = InventoryHistoryEntity.recordOrderDecrease(
     *     productId, currentStock, newStock.intValue(), quantity, orderId
     * );
     * // 결과: [100 → 95] 변경, 차감량 5, 주문 12345로 인한 재고 감소 기록됨
     * </pre>
     *
     * <p><b>데이터 복구 시나리오:</b></p>
     * <p>Redis 장애 발생 시 복구 절차:
     * 1. DB에서 각 상품의 마지막 재고 수량 조회
     * 2. 장애 시점 이후의 모든 ORDER_DECREASE 히스토리 조회
     * 3. 재고 = 마지막 DB 재고 - 모든 ORDER_DECREASE의 quantityChange 합계
     * 4. 복구된 재고를 Redis에 로드</p>
     *
     * <p><b>주의사항:</b></p>
     * <ul>
     *   <li>이 메서드는 히스토리만 생성하며, 실제 재고 차감은 Redis에서 이미 완료된 상태입니다</li>
     *   <li>히스토리 저장 실패 시에도 주문은 계속 진행됩니다 (비즈니스 연속성 우선)</li>
     *   <li>DB 트랜잭션과 독립적으로 동작하여 Redis 재고와 완전히 일치하지 않을 수 있습니다</li>
     * </ul>
     *
     * @param productId 재고가 차감된 상품 ID
     * @param quantityBefore 차감 전 재고 수량
     * @param quantityAfter 차감 후 재고 수량
     * @param quantityChange 차감된 수량
     * @param orderId 재고 차감을 일으킨 주문 ID
     * @return 생성된 재고 변경 이력 엔티티
     */
    public static InventoryHistoryEntity recordOrderDecrease(Long productId, Integer quantityBefore,
        Integer quantityAfter, Integer quantityChange, Long orderId) {
        return InventoryHistoryEntity.builder()
            .productId(productId)
            .changeType(InventoryChangeType.ORDER_DECREASE)
            .quantityBefore(quantityBefore)
            .quantityAfter(quantityAfter)
            .quantityChange(quantityChange)
            .orderId(orderId)
            .reason("주문으로 인한 재고 차감")
            .build();
    }

    /**
     * 주문 만료 또는 취소로 인한 재고 복구 이력을 기록합니다.
     *
     * <p><b>기능 설명:</b></p>
     * <p>미결제 주문이 시간 초과로 만료되거나, 고객이 주문을 취소할 때 차감했던 재고를 복구하는 시점에
     * 이 히스토리를 기록합니다. ORDER_RESTORE 타입으로 저장되어 재고 증가 추적이 가능합니다.</p>
     *
     * <p><b>왜 이 메서드가 필요한가:</b></p>
     * <p>전자상거래에서 주문 생성 시 재고를 즉시 차감하는 것은 일반적인 패턴이지만,
     * 모든 주문이 결제로 이어지는 것은 아닙니다. 통계적으로 주문의 30~50%는 결제 없이 이탈합니다.
     *
     * 이러한 미결제 주문의 재고를 복구하지 않으면:
     * - 판매 가능한 재고가 부정확하게 표시됨
     * - 실제로는 재고가 있지만 "품절"로 보여 매출 손실 발생
     * - 재고 보고서와 실제 재고가 불일치
     *
     * 이 히스토리를 통해 재고 복구도 완전히 추적되어 재고 변동의 전체 그림을 파악할 수 있습니다.</p>
     *
     * <p><b>왜 이렇게 동작하는가:</b></p>
     * <ol>
     *   <li><b>변경 타입:</b> ORDER_RESTORE로 설정하여 주문 관련 복구임을 명시
     *       (RECEIVE와 구분하여 입고가 아닌 주문 취소임을 알 수 있음)</li>
     *   <li><b>변경 전후 수량:</b> 복구 전 95개 → 복구 후 100개 형태로 증가 기록</li>
     *   <li><b>변경 수량:</b> 복구된 수량 저장 (양수)</li>
     *   <li><b>주문 ID:</b> 어떤 주문의 취소/만료로 복구되었는지 추적</li>
     *   <li><b>이유:</b> "주문 만료로 인한 재고 복구" 명시</li>
     * </ol>
     *
     * <p><b>사용 예시:</b></p>
     * <pre>
     * // OrderExpirationScheduler에서 만료된 주문 처리 시
     * Integer currentStock = 95;   // 현재 Redis 재고
     * Long newStock = 100L;        // Redis INCR 후 새 재고
     * Integer quantity = 5;        // 복구할 수량
     * Long orderId = 12345L;       // 만료된 주문 ID
     *
     * InventoryHistoryEntity history = InventoryHistoryEntity.recordOrderRestore(
     *     productId, currentStock, newStock.intValue(), quantity, orderId
     * );
     * // 결과: [95 → 100] 변경, 복구량 5, 주문 12345 만료로 인한 재고 복구 기록됨
     * </pre>
     *
     * <p><b>재고 검증 시나리오:</b></p>
     * <p>재고 불일치 발생 시 검증 방법:
     * 1. 특정 상품의 모든 ORDER_DECREASE 합계 계산
     * 2. 모든 ORDER_RESTORE 합계 계산
     * 3. 이론적 재고 = 초기 재고 - ORDER_DECREASE + ORDER_RESTORE
     * 4. 실제 Redis/DB 재고와 비교하여 불일치 탐지</p>
     *
     * <p><b>주문 생명주기 추적:</b></p>
     * <p>주문 ID로 히스토리를 조회하면 전체 생명주기 파악 가능:
     * - ORDER_DECREASE: 주문 생성 시점 (재고 차감)
     * - ORDER_RESTORE: 주문 만료/취소 시점 (재고 복구)
     * - 둘 다 존재하면 결제되지 않은 주문
     * - DECREASE만 있으면 정상 결제된 주문</p>
     *
     * <p><b>주의사항:</b></p>
     * <ul>
     *   <li>같은 주문 ID로 중복 복구되지 않도록 주문 상태 관리가 중요합니다</li>
     *   <li>복구 시점에는 주문이 이미 만료/취소 상태로 변경되어 있어야 합니다</li>
     *   <li>복구 수량은 원래 차감했던 수량과 정확히 일치해야 데이터 일관성이 유지됩니다</li>
     * </ul>
     *
     * @param productId 재고가 복구된 상품 ID
     * @param quantityBefore 복구 전 재고 수량
     * @param quantityAfter 복구 후 재고 수량
     * @param quantityChange 복구된 수량
     * @param orderId 재고 복구를 일으킨 주문 ID
     * @return 생성된 재고 변경 이력 엔티티
     */
    public static InventoryHistoryEntity recordOrderRestore(Long productId, Integer quantityBefore,
        Integer quantityAfter, Integer quantityChange, Long orderId) {
        return InventoryHistoryEntity.builder()
            .productId(productId)
            .changeType(InventoryChangeType.ORDER_RESTORE)
            .quantityBefore(quantityBefore)
            .quantityAfter(quantityAfter)
            .quantityChange(quantityChange)
            .orderId(orderId)
            .reason("주문 만료로 인한 재고 복구")
            .build();
    }

    // 입출고 관련 이력
    public static InventoryHistoryEntity recordReceive(Long productId, Integer quantityBefore,
        Integer quantityAfter, Integer quantity) {
        return InventoryHistoryEntity.builder()
            .productId(productId)
            .changeType(InventoryChangeType.RECEIVE)
            .quantityBefore(quantityBefore)
            .quantityAfter(quantityAfter)
            .quantityChange(quantity)
            .reason("입고")
            .build();
    }

    public static InventoryHistoryEntity recordSale(Long productId, Integer quantityBefore,
        Integer quantityAfter, Integer quantity) {
        return InventoryHistoryEntity.builder()
            .productId(productId)
            .changeType(InventoryChangeType.SALE)
            .quantityBefore(quantityBefore)
            .quantityAfter(quantityAfter)
            .quantityChange(quantity)
            .reason("판매")
            .build();
    }

    public static InventoryHistoryEntity recordAdjust(Long productId, Integer quantityBefore,
        Integer quantityAfter, Integer quantityChange, String reason) {
        return InventoryHistoryEntity.builder()
            .productId(productId)
            .changeType(InventoryChangeType.ADJUST)
            .quantityBefore(quantityBefore)
            .quantityAfter(quantityAfter)
            .quantityChange(Math.abs(quantityChange))
            .reason(reason != null ? reason : "재고 조정")
            .build();
    }

    // 시스템 동기화
    public static InventoryHistoryEntity recordSync(Long productId, Integer quantityBefore,
        Integer quantityAfter, Integer quantityChange) {
        return InventoryHistoryEntity.builder()
            .productId(productId)
            .changeType(InventoryChangeType.SYNC)
            .quantityBefore(quantityBefore)
            .quantityAfter(quantityAfter)
            .quantityChange(Math.abs(quantityChange))
            .reason("Redis Write-Back 동기화")
            .build();
    }
}
