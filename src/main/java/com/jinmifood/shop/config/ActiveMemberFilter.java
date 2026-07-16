package com.jinmifood.shop.config;

import com.jinmifood.shop.repository.MemberRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class ActiveMemberFilter extends OncePerRequestFilter {
    private final MemberRepository members;
    public ActiveMemberFilter(MemberRepository members){this.members=members;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        boolean member=authentication!=null&&authentication.isAuthenticated()&&authentication.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_MEMBER"));
        if(member){
            var stored=members.findByEmailIgnoreCase(authentication.getName()).orElse(null);
            if(stored!=null&&!stored.isActive()){
                SecurityContextHolder.clearContext();HttpSession session=request.getSession(false);if(session!=null)session.invalidate();
                response.sendRedirect("/login?disabled");return;
            }
            if(stored!=null&&authentication instanceof OAuth2AuthenticationToken&&(stored.getPhone().isBlank()||stored.getBirthDate()==null)&&!allowedDuringPhoneVerification(request)){
                response.sendRedirect(request.getContextPath()+"/social/phone");return;
            }
        }
        chain.doFilter(request,response);
    }
    private boolean allowedDuringPhoneVerification(HttpServletRequest request){
        String path=request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/social/phone")||path.equals("/logout")||path.equals("/error")||path.equals("/favicon.ico")
                ||path.startsWith("/api/sms/")||path.startsWith("/css/")||path.startsWith("/js/")||path.startsWith("/images/")
                ||path.startsWith("/actuator/health");
    }
}
