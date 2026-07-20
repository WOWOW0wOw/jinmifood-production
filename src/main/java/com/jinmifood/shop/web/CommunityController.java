package com.jinmifood.shop.web;
import com.jinmifood.shop.service.CommunityService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
@RequestMapping("/community")
public class CommunityController {
    private final CommunityService community;
    public CommunityController(CommunityService community){this.community=community;}
    @GetMapping String home(Model model){model.addAttribute("notices",community.notices(PageRequest.of(0,5)));return "store/community";}
    @GetMapping("/notices") String notices(@RequestParam(defaultValue="0")int page,Model model){model.addAttribute("notices",community.notices(PageRequest.of(safe(page),15)));return "store/notices";}
    @GetMapping("/notices/{id}") String notice(@PathVariable Long id,Model model){model.addAttribute("notice",community.notice(id));return "store/notice";}
    @GetMapping("/customer-center") String customerCenter(){return "store/customer-center";}
    private int safe(int page){return Math.max(0,Math.min(page,100_000));}
}
