package com.jinmifood.shop.web;

import com.jinmifood.shop.domain.*;
import com.jinmifood.shop.repository.*;
import com.jinmifood.shop.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.NoSuchElementException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final CustomerOrderRepository orders;
    private final ShopService shop;
    private final UploadService uploads;
    private final PaymentService payments;
    private final MemberRepository members;
    private final SiteVisitService siteVisits;

    public AdminController(ProductRepository products,CategoryRepository categories,CustomerOrderRepository orders,
                           ShopService shop,UploadService uploads,PaymentService payments,MemberRepository members,
                           SiteVisitService siteVisits){
        this.products=products;this.categories=categories;this.orders=orders;this.shop=shop;this.uploads=uploads;this.payments=payments;this.members=members;this.siteVisits=siteVisits;
    }

    @GetMapping
    String dashboard(Model model){
        model.addAttribute("recentOrders",orders.findTop8ByOrderByCreatedAtDesc());
        model.addAttribute("orderCount",orders.count());
        model.addAttribute("productCount",products.count());
        model.addAttribute("memberCount",members.count());
        model.addAttribute("pendingCount",orders.countByStatusIn(java.util.List.of(OrderStatus.PAYMENT_PENDING,OrderStatus.PAID)));
        model.addAttribute("revenue",orders.sumTotalAmountByStatusIn(java.util.List.of(OrderStatus.PAID,OrderStatus.PREPARING,OrderStatus.SHIPPED,OrderStatus.DELIVERED)));
        model.addAttribute("todayViews",siteVisits.todayViews());
        model.addAttribute("totalViews",siteVisits.totalViews());
        return "admin/dashboard";
    }

    @GetMapping("/products")
    String products(@RequestParam(defaultValue="0")int page,Model model){
        model.addAttribute("products",products.findAll(PageRequest.of(safePage(page),20,Sort.by(Sort.Direction.DESC,"createdAt"))));
        return "admin/products";
    }

    @GetMapping("/products/new")
    String create(Model model){model.addAttribute("product",new ProductForm());addCategories(model);return "admin/product-form";}

    @GetMapping("/products/{id}/edit")
    String edit(@PathVariable Long id,Model model){model.addAttribute("product",ProductForm.from(products.findById(id).orElseThrow()));addCategories(model);return "admin/product-form";}

    @PostMapping("/products/save")
    String save(@Valid @ModelAttribute("product") ProductForm form,BindingResult errors,Model model,RedirectAttributes redirect){
        boolean duplicate=form.getId()==null?products.existsBySlug(form.getSlug()):products.existsBySlugAndIdNot(form.getSlug(),form.getId());
        if(duplicate)errors.rejectValue("slug","duplicate","이미 사용 중인 URL 식별자입니다.");
        if(errors.hasErrors()){addCategories(model);return "admin/product-form";}
        try{
            var category=categories.findById(form.getCategoryId()).orElseThrow();
            if(form.getId()!=null){
                var target=products.findById(form.getId()).orElseThrow();
                form.applyTo(target);
                target.setCategory(category);
                products.save(target);
            }else{
                var target=new Product();form.applyTo(target);target.setCategory(category);
                target.setImageUrl("/images/product-placeholder.svg");products.save(target);
            }
            redirect.addFlashAttribute("message","상품을 저장했습니다.");
            return "redirect:/admin/products";
        }catch(IllegalArgumentException|IllegalStateException e){
            errors.reject("image",e.getMessage());addCategories(model);return "admin/product-form";
        }
    }

    @PostMapping("/products/{id}/image")
    String replaceImage(@PathVariable Long id,@RequestParam("imageFile") MultipartFile imageFile,RedirectAttributes redirect){
        String uploadedUrl=null;
        try{
            var target=products.findById(id).orElseThrow();String previousUrl=target.getImageUrl();
            uploadedUrl=uploads.saveProductImage(imageFile);
            if(uploadedUrl==null)throw new IllegalArgumentException("교체할 이미지 파일을 선택해 주세요.");
            target.setImageUrl(uploadedUrl);products.save(target);
            if(!java.util.Objects.equals(previousUrl,uploadedUrl)&&products.countByImageUrl(previousUrl)==0)uploads.deleteProductImage(previousUrl);
            redirect.addFlashAttribute("message","상품 이미지만 교체했습니다.");
        }catch(IllegalArgumentException|IllegalStateException e){
            uploads.deleteProductImage(uploadedUrl);redirect.addFlashAttribute("message","이미지 교체 실패: "+e.getMessage());
        }catch(RuntimeException e){uploads.deleteProductImage(uploadedUrl);throw e;}
        return "redirect:/admin/products/"+id+"/edit";
    }

    @GetMapping("/orders")
    String orders(@RequestParam(defaultValue="0") int page,Model model){
        model.addAttribute("orders",orders.findAllByOrderByCreatedAtDesc(PageRequest.of(safePage(page),30)));
        model.addAttribute("statuses",OrderStatus.values());
        return "admin/orders-page";
    }

    @PostMapping("/orders/{id}/status")
    String status(@PathVariable Long id,@RequestParam OrderStatus status,@RequestParam(required=false)Integer returnPage,RedirectAttributes redirect){
        try{
            var order=orders.findById(id).orElseThrow();
            if(status==OrderStatus.CANCELLED&&order.getPaymentStatus()==PaymentStatus.PAID)
                throw new IllegalStateException("승인된 결제는 '결제 취소' 버튼으로 취소해 주세요.");
            if(status==OrderStatus.PAID&&order.getPaymentStatus()!=PaymentStatus.PAID)
                throw new IllegalStateException("토스 승인이 확인되지 않은 주문은 결제완료로 변경할 수 없습니다.");
            shop.changeOrderStatus(id,status);redirect.addFlashAttribute("message","주문 상태를 변경했습니다.");
        }catch(IllegalStateException|NoSuchElementException e){redirect.addFlashAttribute("message","변경 실패: "+e.getMessage());}
        return ordersRedirect(returnPage);
    }

    @PostMapping("/orders/{id}/payment/cancel")
    String cancelPayment(@PathVariable Long id,@RequestParam(defaultValue="고객 요청에 따른 주문 취소") String reason,
                         @RequestParam(required=false)Integer returnPage,RedirectAttributes redirect){
        try{payments.cancel(id,reason);redirect.addFlashAttribute("message","토스 결제와 주문을 모두 취소했습니다.");}
        catch(PaymentException|IllegalStateException|NoSuchElementException e){redirect.addFlashAttribute("message","결제 취소 실패: "+e.getMessage());}
        return ordersRedirect(returnPage);
    }

    private void addCategories(Model model){model.addAttribute("allCategories",categories.findAllByOrderByDisplayOrderAsc());}
    private int safePage(int page){return Math.max(0,Math.min(page,100_000));}
    private String ordersRedirect(Integer page){return page==null?"redirect:/admin/orders":"redirect:/admin/orders?page="+safePage(page);}
}
