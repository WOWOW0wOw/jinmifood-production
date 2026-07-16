package com.jinmifood.shop.service;

import com.jinmifood.shop.domain.Member;
import com.jinmifood.shop.domain.OrderStatus;
import com.jinmifood.shop.repository.CustomerOrderRepository;
import com.jinmifood.shop.repository.MemberRepository;
import com.jinmifood.shop.repository.ProductRepository;
import com.jinmifood.shop.web.Cart;
import com.jinmifood.shop.web.CheckoutForm;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderRetentionServiceTest {
    @Autowired OrderRetentionService retention;
    @Autowired ShopService shop;
    @Autowired ProductRepository products;
    @Autowired CustomerOrderRepository orders;
    @Autowired MemberRepository members;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void deletesOnlyExpiredUnpaidOrderAndRestoresStock() {
        var product = products.findByActiveTrueOrderByCreatedAtDesc().getFirst();
        int stockBeforeOrder = product.getStock();
        var order = placeOrder(product.getId(), null, 0);
        age(order.getId(), Duration.ofDays(8));

        var result = retention.cleanupExpiredUnpaidOrders(Duration.ofDays(7), 100);

        assertThat(result.deletedOrders()).isEqualTo(1);
        assertThat(result.restoredStock()).isEqualTo(1);
        assertThat(orders.findById(order.getId())).isEmpty();
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(stockBeforeOrder);
    }

    @Test
    void restoresMemberPointsUsedByExpiredOrder() {
        var member = members.save(new Member("retention@example.com", "unused", "정리회원", "010-2222-3333"));
        member.addPoints(5_000);
        var product = products.findByActiveTrueOrderByCreatedAtDesc().getFirst();
        var order = placeOrder(product.getId(), member.getEmail(), 1_000);
        assertThat(member.getPoints()).isEqualTo(4_000);
        age(order.getId(), Duration.ofDays(8));

        var result = retention.cleanupExpiredUnpaidOrders(Duration.ofDays(7), 100);

        assertThat(result.restoredPoints()).isEqualTo(1_000);
        assertThat(members.findById(member.getId()).orElseThrow().getPoints()).isEqualTo(5_000);
    }

    @Test
    void keepsPaidAndRecentOrders() {
        var product = products.findByActiveTrueOrderByCreatedAtDesc().getFirst();
        var paid = placeOrder(product.getId(), null, 0);
        shop.changeOrderStatus(paid.getId(), OrderStatus.PAID);
        age(paid.getId(), Duration.ofDays(30));
        var recent = placeOrder(product.getId(), null, 0);

        var result = retention.cleanupExpiredUnpaidOrders(Duration.ofDays(7), 100);

        assertThat(result.deletedOrders()).isZero();
        assertThat(orders.findById(paid.getId())).isPresent();
        assertThat(orders.findById(recent.getId())).isPresent();
    }

    private com.jinmifood.shop.domain.CustomerOrder placeOrder(Long productId, String memberEmail, int points) {
        var cart = new Cart();
        cart.add(productId, 1);
        var form = new CheckoutForm();
        form.setCustomerName("데이터 정리 테스트");
        form.setPhone("010-1234-5678");
        form.setEmail(memberEmail == null ? "cleanup@example.com" : memberEmail);
        form.setPostalCode("07200");
        form.setAddress("서울시 영등포구");
        form.setPointsToUse(points);
        form.setAgreed(true);
        return shop.placeOrder(cart, form, memberEmail);
    }

    private void age(Long orderId, Duration age) {
        orders.flush();
        jdbc.update("update customer_orders set created_at=? where id=?",
            Timestamp.valueOf(LocalDateTime.now().minus(age)), orderId);
        entityManager.clear();
    }
}
