package com.jinmifood.shop.web;
import com.jinmifood.shop.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import com.jinmifood.shop.service.CommunityService;

@Controller
public class StoreController {
    private final ShopService shop; private final CommunityService community;
    public StoreController(ShopService shop,CommunityService community){this.shop=shop;this.community=community;}
    @GetMapping("/") String home(Model model){
        model.addAttribute("featured",shop.featured());
        model.addAttribute("heroProduct",shop.heroProduct());
        return "store/home";
    }
    @GetMapping("/products") String products(@RequestParam(required=false)String category,@RequestParam(required=false)String q,
        @RequestParam(defaultValue="0")int page,Model model){
        var pageable=PageRequest.of(safePage(page),20,Sort.by(Sort.Direction.DESC,"createdAt"));
        model.addAttribute("products",shop.products(category,q,pageable));model.addAttribute("selectedCategory",category);model.addAttribute("query",q);return "store/products";
    }
    @GetMapping("/products/{slug}") String product(@PathVariable String slug,@RequestParam(defaultValue="0")int reviewPage,
        @RequestParam(defaultValue="0")int inquiryPage,Authentication auth,Model model){
        var product=shop.product(slug);model.addAttribute("product",product);
        model.addAttribute("reviews",community.reviews(product.getId(),PageRequest.of(safePage(reviewPage),5)));
        model.addAttribute("inquiries",community.inquiries(product.getId(),PageRequest.of(safePage(inquiryPage),5)));
        model.addAttribute("reviewCount",community.reviewCount(product.getId()));model.addAttribute("averageRating",community.averageRating(product.getId()));
        model.addAttribute("canReview",auth!=null&&community.canReview(auth.getName(),product.getId()));
        model.addAttribute("currentEmail",auth==null?null:auth.getName());return "store/product";
    }
    @GetMapping("/about") String about(){return "store/about";}
    private int safePage(int page){return Math.max(0,Math.min(page,100_000));}
}
