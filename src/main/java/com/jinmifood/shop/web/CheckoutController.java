package com.jinmifood.shop.web;
import com.jinmifood.shop.service.ShopService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.jinmifood.shop.repository.MemberRepository;

@Controller
public class CheckoutController {
    private final ShopService shop; private final MemberRepository members; public CheckoutController(ShopService shop,MemberRepository members){this.shop=shop;this.members=members;}
    private Cart cart(HttpSession s){var c=(Cart)s.getAttribute("cart");return c==null?new Cart():c;}
    @GetMapping("/checkout") String checkout(HttpSession s,Model m,Authentication auth){var c=cart(s);if(c.isEmpty())return "redirect:/cart";var member=member(auth);if(!m.containsAttribute("checkoutForm")){var form=new CheckoutForm();member.ifPresent(v->{form.setCustomerName(v.getName());form.setPhone(v.getPhone());form.setEmail(v.getEmail());form.setPostalCode(v.getPostalCode());form.setAddress(v.getAddress());form.setAddressDetail(v.getAddressDetail());form.setRememberAddress(true);});m.addAttribute("checkoutForm",form);}addCheckoutModel(m,c,member.orElse(null));return "store/checkout-v2";}
    @PostMapping("/checkout") String place(@Valid @ModelAttribute CheckoutForm checkoutForm,BindingResult errors,HttpSession s,Model m,Authentication auth){var c=cart(s);var member=member(auth);if(errors.hasErrors()){addCheckoutModel(m,c,member.orElse(null));return "store/checkout-v2";}try{var order=shop.placeOrder(c,checkoutForm,member.map(v->v.getEmail()).orElse(null));member.filter(v->checkoutForm.isRememberAddress()).ifPresent(v->{v.updateDefaultAddress(checkoutForm.getPostalCode(),checkoutForm.getAddress(),checkoutForm.getAddressDetail());members.save(v);});c.clear();s.setAttribute("pendingPaymentOrderNumber",order.getOrderNumber());return "redirect:/payments/toss/request/"+order.getOrderNumber();}catch(IllegalStateException e){errors.reject("order",e.getMessage());addCheckoutModel(m,c,member.orElse(null));return "store/checkout-v2";}}
    private java.util.Optional<com.jinmifood.shop.domain.Member> member(Authentication auth){if(auth==null||auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_MEMBER")))return java.util.Optional.empty();return members.findByEmailIgnoreCase(auth.getName());}
    private void addCheckoutModel(Model model,Cart cart,com.jinmifood.shop.domain.Member member){model.addAttribute("cart",shop.cartView(cart));model.addAttribute("member",member);model.addAttribute("availablePoints",member==null?0:Math.max(0,member.getPoints()));}
}
