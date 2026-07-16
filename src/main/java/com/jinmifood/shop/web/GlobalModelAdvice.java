package com.jinmifood.shop.web;
import com.jinmifood.shop.service.ShopService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@ControllerAdvice
public class GlobalModelAdvice {
    private final ShopService shop;
    public GlobalModelAdvice(ShopService shop){this.shop=shop;}
    @ModelAttribute("categories") Object categories(){return shop.categories();}
    @ModelAttribute("cartCount") int cartCount(HttpSession session){var cart=(Cart)session.getAttribute("cart");return cart==null?0:cart.getCount();}
    @ModelAttribute("memberLoggedIn") boolean memberLoggedIn(Authentication auth){return auth!=null&&auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_MEMBER"));}
    @ModelAttribute("loggedIn") boolean loggedIn(Authentication auth){return auth!=null&&auth.isAuthenticated();}
}
