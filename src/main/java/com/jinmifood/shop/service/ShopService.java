package com.jinmifood.shop.service;
import com.jinmifood.shop.domain.*;
import com.jinmifood.shop.repository.*;
import com.jinmifood.shop.web.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service @Transactional(readOnly=true)
public class ShopService {
    public static final long FREE_SHIPPING_THRESHOLD=50_000, SHIPPING_FEE=3_500;
    private final ProductRepository products; private final CategoryRepository categories; private final CustomerOrderRepository orders; private final MemberRepository members;
    public ShopService(ProductRepository p,CategoryRepository c,CustomerOrderRepository o,MemberRepository m){products=p;categories=c;orders=o;members=m;}
    public List<Category> categories(){return categories.findAllByOrderByDisplayOrderAsc();}
    public List<Product> featured(){return products.findTop8ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc();}
    public Page<Product> products(String category,String query,Pageable pageable){
        if(query!=null&&!query.isBlank())return products.findByActiveTrueAndNameContainingIgnoreCase(query.trim(),pageable);
        if(category!=null&&!category.isBlank())return products.findByActiveTrueAndCategorySlug(category,pageable);
        return products.findByActiveTrue(pageable);
    }
    public Product product(String slug){return products.findBySlugAndActiveTrue(slug).orElseThrow(()->new NoSuchElementException("상품을 찾을 수 없습니다."));}
    public CartView cartView(Cart cart){
        var lines=new ArrayList<CartView.Line>(); long subtotal=0;
        for(var e:new ArrayList<>(cart.getQuantities().entrySet())){
            var p=products.findById(e.getKey()).orElse(null);
            if(p==null||!p.isActive()||p.getStock()<=0){cart.update(e.getKey(),0);continue;}
            int q=Math.min(e.getValue(),p.getStock());
            if(q!=e.getValue())cart.update(e.getKey(),q);
            long line=Math.multiplyExact(p.getPrice().longValue(),q);lines.add(new CartView.Line(p,q,line));subtotal=Math.addExact(subtotal,line);
        }
        long shipping=subtotal==0||subtotal>=FREE_SHIPPING_THRESHOLD?0:SHIPPING_FEE;return new CartView(lines,subtotal,shipping,Math.addExact(subtotal,shipping));
    }
    @Transactional public CustomerOrder placeOrder(Cart cart,CheckoutForm f,String memberEmail){
        if(cart.isEmpty())throw new IllegalStateException("장바구니가 비어 있습니다.");
        String number="JM"+java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+UUID.randomUUID().toString().substring(0,6).toUpperCase();
        long subtotal=0;var locked=new ArrayList<Map.Entry<Product,Integer>>();
        for(var e:cart.getQuantities().entrySet()){var p=products.findForUpdate(e.getKey()).orElseThrow(()->new IllegalStateException("판매 종료된 상품이 있습니다."));if(!p.isActive())throw new IllegalStateException("판매 종료된 상품이 있습니다.");p.decreaseStock(e.getValue());subtotal=Math.addExact(subtotal,Math.multiplyExact(p.getPrice().longValue(),e.getValue()));locked.add(Map.entry(p,e.getValue()));}
        long shipping=subtotal>=FREE_SHIPPING_THRESHOLD?0:SHIPPING_FEE;
        Member member=memberEmail==null?null:members.findByEmailForUpdate(memberEmail).orElseThrow(()->new IllegalStateException("회원 정보를 찾을 수 없습니다."));
        int pointsUsed=f.getPointsToUse();
        if(member==null&&pointsUsed>0)throw new IllegalStateException("포인트는 로그인한 회원만 사용할 수 있습니다.");
        if(pointsUsed>subtotal)throw new IllegalStateException("포인트는 상품금액을 초과하여 사용할 수 없습니다.");
        if(member!=null)member.usePoints(pointsUsed);
        var order=new CustomerOrder(number,f.getCustomerName(),f.getPhone(),f.getEmail(),f.getPostalCode(),f.getAddress(),f.getAddressDetail(),f.getDeliveryMemo(),subtotal,shipping,member,pointsUsed);
        locked.forEach(e->order.addItem(new OrderItem(e.getKey(),e.getValue())));return orders.save(order);
    }
    @Transactional public void changeOrderStatus(Long id,OrderStatus next){
        var order=orders.findById(id).orElseThrow(()->new NoSuchElementException("주문이 없습니다."));
        var previous=order.getStatus();
        if(previous==next)return;
        if(!previous.canTransitionTo(next))throw new IllegalStateException(previous.getLabel()+" 상태에서 "+next.getLabel()+" 상태로 변경할 수 없습니다.");
        if(next==OrderStatus.CANCELLED){
            if(order.getMember()!=null){int earned=order.getPointsEarned();order.getMember().removePoints(earned);order.getMember().addPoints(order.getPointsUsed());order.revokePoints();}
            order.getItems().forEach(i->products.findForUpdate(i.getProductId()).ifPresent(p->p.increaseStock(i.getQuantity())));
        }
        if(next==OrderStatus.PAID&&order.getMember()!=null&&order.getPointsEarned()==0){int earned=(int)Math.min(Integer.MAX_VALUE,Math.max(0,order.getSubtotal()-order.getPointsUsed())/10);order.getMember().addPoints(earned);order.awardPoints(earned);}
        order.changeStatus(next);
    }
}
