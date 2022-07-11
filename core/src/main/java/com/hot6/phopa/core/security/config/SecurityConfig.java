package com.hot6.phopa.core.security.config;

import com.hot6.phopa.core.security.service.PrincipalOauth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity //spring security filter가 filterChain에 등록
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final PrincipalOauth2UserService principalOauth2UserService;

    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
//        http
//                .authorizeRequests()
//                .antMatchers("/admin/v1/test/").authenticated()
//                .antMatchers("/test/login").authenticated()
//                .antMatchers("/admin/v1/test/**").hasAnyAuthority(UserRole.ANONYMOUS.getAuthority(), UserRole.USER.getAuthority()) //회원가입은 누구나 통과될 수 있도록 설정
//                .antMatchers("/admin/v1/user/**").permitAll()
//                .anyRequest().permitAll()
//                .and()
//                .formLogin()
//                .loginPage("/admin/v1/login")
//                .loginProcessingUrl("/login")
//                .usernameParameter("email")
//                .and()
//                .oauth2Login()
//                .loginPage("/admin/v1/login")
//                .defaultSuccessUrl("/test/login")
//                .userInfoEndpoint()
//                .userService(principalOauth2UserService) //oauth 로그인 완료 이후 후처리
//                ;
        http.authorizeRequests().anyRequest().permitAll();
        //spring security 권한 상관없도록 주석 처리.
    }
}