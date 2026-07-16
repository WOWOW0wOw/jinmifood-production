package com.jinmifood.shop.web;
import com.jinmifood.shop.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class StoreController {
    private final ShopService shop; public StoreController(ShopService shop){this.shop=shop;}
    @GetMapping("/") String home(Model model){model.addAttribute("featured",shop.featured());return "store/home";}
    @GetMapping("/products") String products(@RequestParam(required=false)String category,@RequestParam(required=false)String q,Model model){model.addAttribute("products",shop.products(category,q));model.addAttribute("selectedCategory",category);model.addAttribute("query",q);return "store/products";}
    @GetMapping("/products/{slug}") String product(@PathVariable String slug,Model model){model.addAttribute("product",shop.product(slug));return "store/product";}
    @GetMapping("/about") String about(){return "store/about";}
}
