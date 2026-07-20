package com.jinmifood.shop.repository;
import com.jinmifood.shop.domain.CustomerOrder;
import com.jinmifood.shop.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.jinmifood.shop.domain.PaymentStatus;
import java.time.LocalDateTime;
import java.util.*;
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder,Long>{
    List<CustomerOrder> findTop8ByOrderByCreatedAtDesc();
    Page<CustomerOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Optional<CustomerOrder> findByOrderNumberAndPhone(String orderNumber,String phone);
    Optional<CustomerOrder> findByOrderNumber(String orderNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.orderNumber=:orderNumber")
    Optional<CustomerOrder> findByOrderNumberForUpdate(@Param("orderNumber") String orderNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id=:id")
    Optional<CustomerOrder> findByIdForUpdate(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.status=:status and o.paymentStatus in :paymentStatuses and o.createdAt < :cutoff order by o.createdAt asc")
    List<CustomerOrder> findCleanupCandidates(@Param("status") OrderStatus status,
        @Param("paymentStatuses") Collection<PaymentStatus> paymentStatuses,
        @Param("cutoff") LocalDateTime cutoff, Pageable pageable);
    @Query("select o from CustomerOrder o where o.member.id=:memberId order by o.createdAt desc")
    Page<CustomerOrder> findMemberOrders(@Param("memberId") Long memberId, Pageable pageable);
    List<CustomerOrder> findTop20ByMemberIdOrderByCreatedAtDesc(Long memberId);
    long countByMemberId(Long memberId);
    @Query("select coalesce(sum(o.totalAmount),0) from CustomerOrder o where o.member.id=:memberId and o.paymentStatus=:paymentStatus")
    long sumPaidAmountByMemberId(@Param("memberId") Long memberId,@Param("paymentStatus") PaymentStatus paymentStatus);
    @Query("select count(i) from CustomerOrder o join o.items i where o.member.id=:memberId and i.productId=:productId and o.paymentStatus=:paymentStatus")
    long countPurchasedProduct(@Param("memberId")Long memberId,@Param("productId")Long productId,@Param("paymentStatus")PaymentStatus paymentStatus);
    long countByStatusIn(Collection<OrderStatus> statuses);
    @Query("select coalesce(sum(o.totalAmount),0) from CustomerOrder o where o.status in :statuses")
    long sumTotalAmountByStatusIn(Collection<OrderStatus> statuses);
}
