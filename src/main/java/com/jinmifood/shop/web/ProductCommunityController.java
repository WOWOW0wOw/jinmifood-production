package com.jinmifood.shop.web;

import com.jinmifood.shop.repository.ProductRepository;
import com.jinmifood.shop.service.CommunityService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/products/{productId}")
public class ProductCommunityController {
    private final CommunityService community; private final ProductRepository products;
    public ProductCommunityController(CommunityService community,ProductRepository products){this.community=community;this.products=products;}
    @PostMapping("/reviews") String review(@PathVariable Long productId,@RequestParam int rating,@RequestParam String content,Authentication auth,RedirectAttributes redirect){
        return act(productId,"reviews",redirect,()->community.addReview(auth.getName(),productId,rating,content),"리뷰를 등록했습니다.");
    }
    @PostMapping("/reviews/{reviewId}/delete") String deleteReview(@PathVariable Long productId,@PathVariable Long reviewId,Authentication auth,RedirectAttributes redirect){
        return act(productId,"reviews",redirect,()->community.deleteReview(auth.getName(),isAdmin(auth),reviewId),"리뷰를 삭제했습니다.");
    }
    @PostMapping("/inquiries") String inquiry(@PathVariable Long productId,@RequestParam String title,@RequestParam String content,Authentication auth,RedirectAttributes redirect){
        return act(productId,"inquiries",redirect,()->community.addInquiry(auth.getName(),productId,title,content),"상품 문의를 등록했습니다.");
    }
    @PostMapping("/inquiries/{inquiryId}/delete") String deleteInquiry(@PathVariable Long productId,@PathVariable Long inquiryId,Authentication auth,RedirectAttributes redirect){
        return act(productId,"inquiries",redirect,()->community.deleteInquiry(auth.getName(),isAdmin(auth),inquiryId),"상품 문의를 삭제했습니다.");
    }
    private String act(Long productId,String anchor,RedirectAttributes redirect,Runnable action,String success){
        var product=products.findById(productId).orElseThrow();
        try{action.run();redirect.addFlashAttribute("message",success);}catch(IllegalArgumentException|IllegalStateException e){redirect.addFlashAttribute("message",e.getMessage());}
        return "redirect:/products/"+product.getSlug()+"#"+anchor;
    }
    private boolean isAdmin(Authentication auth){return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));}
}
