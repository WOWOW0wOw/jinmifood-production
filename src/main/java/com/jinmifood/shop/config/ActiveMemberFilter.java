package com.jinmifood.shop.config;

import com.jinmifood.shop.repository.MemberRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
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
        if(member&&members.findByEmailIgnoreCase(authentication.getName()).filter(m->!m.isActive()).isPresent()){
            SecurityContextHolder.clearContext();HttpSession session=request.getSession(false);if(session!=null)session.invalidate();
            response.sendRedirect("/login?disabled");return;
        }
        chain.doFilter(request,response);
    }
}
