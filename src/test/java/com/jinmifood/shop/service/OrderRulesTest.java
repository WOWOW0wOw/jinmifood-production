package com.jinmifood.shop.service;

import com.jinmifood.shop.domain.Member;
import com.jinmifood.shop.domain.OrderStatus;
import com.jinmifood.shop.domain.CustomerOrder;
import com.jinmifood.shop.domain.PaymentStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderRulesTest {
    @Test void onlyAllowsForwardOperationalTransitions(){
        assertThat(OrderStatus.PAYMENT_PENDING.canTransitionTo(OrderStatus.PAID)).isTrue();
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.PREPARING)).isTrue();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.PAID)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PAID)).isFalse();
    }

    @Test void pointsCannotBecomeNegativeWhenRewardsAreClawedBack(){
        var member=new Member("member@example.com","hash","회원","010-1111-2222");member.addPoints(100);
        assertThatThrownBy(()->member.removePoints(101)).isInstanceOf(IllegalStateException.class).hasMessageContaining("취소");
        assertThat(member.getPoints()).isEqualTo(100);
    }

    @Test void legacyOrderWithoutPaymentStatusGetsSafeDerivedStatus()throws Exception{
        var order=new CustomerOrder("JM-LEGACY","과거 주문","010-1111-2222","legacy@example.com","07200","서울시 영등포구",null,null,10000,3500,null,0);
        var field=CustomerOrder.class.getDeclaredField("paymentStatus");field.setAccessible(true);field.set(order,null);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.READY);
        order.changeStatus(OrderStatus.PAID);assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }
}
