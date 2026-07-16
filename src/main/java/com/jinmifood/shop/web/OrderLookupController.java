package com.jinmifood.shop.web;

import com.jinmifood.shop.repository.CustomerOrderRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class OrderLookupController {
    private final CustomerOrderRepository orders;
    public OrderLookupController(CustomerOrderRepository orders){this.orders=orders;}

    @GetMapping("/order-lookup")
    String form(){return "store/order-lookup";}

    @PostMapping("/order-lookup")
    String lookup(@RequestParam String orderNumber,@RequestParam String phone,Model model){
        String normalizedNumber=orderNumber==null?"":orderNumber.trim().toUpperCase();
        String normalizedPhone=phone==null?"":phone.trim();
        model.addAttribute("orderNumber",normalizedNumber);
        model.addAttribute("phone",normalizedPhone);
        orders.findByOrderNumberAndPhone(normalizedNumber,normalizedPhone)
            .ifPresentOrElse(order->model.addAttribute("order",order),()->model.addAttribute("lookupError","주문번호와 연락처가 일치하는 주문을 찾지 못했습니다."));
        return "store/order-lookup";
    }

    @GetMapping("/policies")
    String policies(){return "store/policies";}
}
