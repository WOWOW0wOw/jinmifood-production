package com.jinmifood.shop.web;
import com.jinmifood.shop.service.ShopService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {
    private final ShopService shop; public CartController(ShopService shop){this.shop=shop;}
    private Cart cart(HttpSession s){var c=(Cart)s.getAttribute("cart");if(c==null){c=new Cart();s.setAttribute("cart",c);}return c;}
    @GetMapping("/cart") String cart(HttpSession s,Model m){m.addAttribute("cart",shop.cartView(cart(s)));return "store/cart";}
    @PostMapping("/cart/add") String add(@RequestParam Long productId,@RequestParam(defaultValue="1")int quantity,@RequestParam(defaultValue="/cart")String returnUrl,HttpSession s,RedirectAttributes ra){
        cart(s).add(productId,quantity);ra.addFlashAttribute("message","장바구니에 담았습니다.");
        return "redirect:"+safeReturnUrl(returnUrl);
    }
    @PostMapping("/cart/update") String update(@RequestParam Long productId,@RequestParam int quantity,HttpSession s){cart(s).update(productId,quantity);return "redirect:/cart";}
    private String safeReturnUrl(String value){
        if(value==null||value.contains("\r")||value.contains("\n")||value.startsWith("//"))return "/cart";
        return value.equals("/cart")||value.startsWith("/products/")?value:"/cart";
    }
}
