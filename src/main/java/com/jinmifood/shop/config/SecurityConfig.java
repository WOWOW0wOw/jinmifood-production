package com.jinmifood.shop.config;
import com.jinmifood.shop.repository.MemberRepository;
import com.jinmifood.shop.service.SocialOAuth2UserService;
import com.jinmifood.shop.service.SocialOidcUserService;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.http.HttpMethod;
import java.time.Duration;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean UserDetailsService users(MemberRepository members){
        return login->{
            var member=members.findByEmailIgnoreCase(login).orElseThrow(()->new UsernameNotFoundException("가입되지 않은 이메일입니다."));
            var roles=member.isAdmin()?new String[]{"MEMBER","ADMIN"}:new String[]{"MEMBER"};
            return User.withUsername(member.getEmail()).password(member.getPasswordHash()).disabled(!member.isActive()).roles(roles).build();
        };
    }
    @Bean SecurityFilterChain security(HttpSecurity http,SocialOAuth2UserService oauth2Users,SocialOidcUserService oidcUsers,SocialLoginSuccessHandler socialSuccess,ActiveMemberFilter activeMembers)throws Exception{
        http.authorizeHttpRequests(a->a.requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,"/products/*/reviews","/products/*/reviews/*/delete","/products/*/inquiries","/products/*/inquiries/*/delete").hasRole("MEMBER")
                .requestMatchers("/mypage/**","/social/phone").hasRole("MEMBER")
                .requestMatchers("/actuator/health","/actuator/health/**").permitAll()
                .requestMatchers("/actuator/**").denyAll()
                .anyRequest().permitAll())
            .csrf(c->c.ignoringRequestMatchers("/webhooks/toss"))
            .formLogin(f->f.loginPage("/login").defaultSuccessUrl("/",false).permitAll())
            .oauth2Login(o->o.loginPage("/login").successHandler(socialSuccess).failureUrl("/login?socialError")
                    .userInfoEndpoint(u->u.userService(oauth2Users).oidcUserService(oidcUsers)))
            .logout(l->l.logoutSuccessUrl("/").permitAll())
            .headers(h->h
                .contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; style-src 'self' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https:; script-src 'self' https://js.tosspayments.com; connect-src 'self' https://*.tosspayments.com; frame-src https://*.tosspayments.com; form-action 'self'; frame-ancestors 'none'; base-uri 'self'"))
                .referrerPolicy(r->r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts->hsts.includeSubDomains(true).preload(true).maxAgeInSeconds(Duration.ofDays(365).getSeconds()))
                .permissionsPolicyHeader(p->p.policy("camera=(), microphone=(), geolocation=(), payment=(self), usb=()")));
        http.addFilterBefore(activeMembers,AuthorizationFilter.class);
        return http.build();
    }
}
