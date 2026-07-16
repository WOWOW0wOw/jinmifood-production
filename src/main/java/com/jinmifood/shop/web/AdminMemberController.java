package com.jinmifood.shop.web;

import com.jinmifood.shop.domain.PaymentStatus;
import com.jinmifood.shop.repository.*;
import com.jinmifood.shop.service.MemberAdminService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/members")
public class AdminMemberController {
    private final MemberRepository members;private final SocialAccountRepository socialAccounts;
    private final CustomerOrderRepository orders;private final MemberAdminActionRepository actions;private final MemberAdminService memberAdmin;
    public AdminMemberController(MemberRepository members,SocialAccountRepository socialAccounts,CustomerOrderRepository orders,
                                 MemberAdminActionRepository actions,MemberAdminService memberAdmin){
        this.members=members;this.socialAccounts=socialAccounts;this.orders=orders;this.actions=actions;this.memberAdmin=memberAdmin;
    }
    @GetMapping
    String list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Model model){
        String query=q.trim();int safePage=Math.max(0,page);
        var result=members.search(query,PageRequest.of(safePage,30,Sort.by(Sort.Direction.DESC,"createdAt")));
        var memberIds=result.getContent().stream().map(com.jinmifood.shop.domain.Member::getId).toList();
        Map<Long,List<String>> providers=memberIds.isEmpty()?Map.of():socialAccounts.findByMemberIds(memberIds).stream()
            .collect(Collectors.groupingBy(a->a.getMember().getId(),LinkedHashMap::new,Collectors.mapping(a->a.getProvider().toUpperCase(),Collectors.toList())));
        model.addAttribute("members",result);model.addAttribute("providers",providers);model.addAttribute("q",query);return "admin/members";
    }
    @GetMapping("/{id}")
    String detail(@PathVariable Long id,Model model){
        var member=members.findById(id).orElseThrow();
        model.addAttribute("member",member);model.addAttribute("socialAccounts",socialAccounts.findByMemberIdOrderByCreatedAtAsc(id));
        model.addAttribute("orders",orders.findTop20ByMemberIdOrderByCreatedAtDesc(id));model.addAttribute("orderCount",orders.countByMemberId(id));
        model.addAttribute("paidAmount",orders.sumPaidAmountByMemberId(id,PaymentStatus.PAID));
        model.addAttribute("actions",actions.findTop30ByMemberIdOrderByCreatedAtDesc(id));return "admin/member-detail";
    }
    @PostMapping("/{id}/status")
    String status(@PathVariable Long id,@RequestParam boolean active,@RequestParam(required=false) String reason,Authentication auth,RedirectAttributes redirect){
        return perform(id,redirect,()->memberAdmin.changeActive(id,active,auth.getName(),reason),"회원 상태를 변경했습니다.");
    }
    @PostMapping("/{id}/admin")
    String admin(@PathVariable Long id,@RequestParam boolean admin,@RequestParam(required=false) String reason,Authentication auth,RedirectAttributes redirect){
        return perform(id,redirect,()->memberAdmin.changeAdmin(id,admin,auth.getName(),reason),"관리자 권한을 변경했습니다.");
    }
    @PostMapping("/{id}/points")
    String points(@PathVariable Long id,@RequestParam int delta,@RequestParam String reason,Authentication auth,RedirectAttributes redirect){
        return perform(id,redirect,()->memberAdmin.adjustPoints(id,delta,auth.getName(),reason),"포인트를 조정했습니다.");
    }
    private String perform(Long id,RedirectAttributes redirect,Runnable action,String success){
        try{action.run();redirect.addFlashAttribute("message",success);}catch(IllegalArgumentException|IllegalStateException|java.util.NoSuchElementException e){redirect.addFlashAttribute("message","변경 실패: "+e.getMessage());}
        return "redirect:/admin/members/"+id;
    }
}
