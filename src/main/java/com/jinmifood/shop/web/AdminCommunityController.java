package com.jinmifood.shop.web;
import com.jinmifood.shop.service.CommunityService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/admin")
public class AdminCommunityController {
    private final CommunityService community;
    public AdminCommunityController(CommunityService community){this.community=community;}
    @GetMapping("/inquiries") String inquiries(@RequestParam(defaultValue="0")int page,Model model){model.addAttribute("inquiries",community.allInquiries(PageRequest.of(safe(page),20)));return "admin/inquiries";}
    @PostMapping("/inquiries/{id}/answer") String answer(@PathVariable Long id,@RequestParam String answer,Authentication auth,RedirectAttributes redirect){
        try{community.answerInquiry(id,answer,auth.getName());redirect.addFlashAttribute("message","문의 답변을 저장했습니다.");}catch(IllegalArgumentException|IllegalStateException e){redirect.addFlashAttribute("message",e.getMessage());}
        return "redirect:/admin/inquiries";
    }
    @PostMapping("/inquiries/{id}/delete") String deleteInquiry(@PathVariable Long id,Authentication auth,RedirectAttributes redirect){community.deleteInquiry(auth.getName(),true,id);redirect.addFlashAttribute("message","문의를 삭제했습니다.");return "redirect:/admin/inquiries";}
    @GetMapping("/notices") String notices(@RequestParam(defaultValue="0")int page,Model model){model.addAttribute("notices",community.notices(PageRequest.of(safe(page),20)));return "admin/notices";}
    @GetMapping("/notices/new") String newNotice(Model model){model.addAttribute("noticeId",null);model.addAttribute("title","");model.addAttribute("content","");model.addAttribute("pinned",false);return "admin/notice-form";}
    @GetMapping("/notices/{id}/edit") String editNotice(@PathVariable Long id,Model model){var n=community.notice(id);model.addAttribute("noticeId",n.getId());model.addAttribute("title",n.getTitle());model.addAttribute("content",n.getContent());model.addAttribute("pinned",n.isPinned());return "admin/notice-form";}
    @PostMapping("/notices/save") String saveNotice(@RequestParam(required=false)Long id,@RequestParam String title,@RequestParam String content,@RequestParam(defaultValue="false")boolean pinned,RedirectAttributes redirect){
        try{community.saveNotice(id,title,content,pinned);redirect.addFlashAttribute("message","공지사항을 저장했습니다.");return "redirect:/admin/notices";}catch(IllegalArgumentException e){redirect.addFlashAttribute("message",e.getMessage());return id==null?"redirect:/admin/notices/new":"redirect:/admin/notices/"+id+"/edit";}
    }
    @PostMapping("/notices/{id}/delete") String deleteNotice(@PathVariable Long id,RedirectAttributes redirect){community.deleteNotice(id);redirect.addFlashAttribute("message","공지사항을 삭제했습니다.");return "redirect:/admin/notices";}
    private int safe(int page){return Math.max(0,Math.min(page,100_000));}
}
