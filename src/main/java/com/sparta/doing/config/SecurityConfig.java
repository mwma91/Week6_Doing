package com.sparta.doing.config;

import com.sparta.doing.entity.Authority;
import com.sparta.doing.jwt.JwtAccessDeniedHandler;
import com.sparta.doing.jwt.JwtAuthenticationEntryPoint;
import com.sparta.doing.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig /*implements WebMvcConfigurer*/ {
    private final TokenProvider tokenProvider;

    private final CorsFilter corsFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    // @Override
    // public void addCorsMappings(CorsRegistry registry) {
    //     registry.addMapping("/**")
    //             .allowedOriginPatterns("/**")
    //             .allowedMethods("*")
    //             .allowedHeaders("*")
    //             .exposedHeaders("*")
    //             .allowCredentials(true);
    // }

    // @Bean
    // public CorsConfigurationSource corsConfigurationSource() {
    //     CorsConfiguration configuration = new CorsConfiguration();
    //
    //     // configuration.addAllowedOrigin("http://localhost:3000");
    //     configuration.addAllowedOrigin("*");
    //     configuration.addAllowedHeader("*");
    //     configuration.addAllowedMethod("*");
    //
    //     configuration.addExposedHeader("accessToken");
    //     configuration.addExposedHeader("Set-Cookie");
    //
    //     configuration.setAllowCredentials(true);
    //     UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    //     source.registerCorsConfiguration("/**", configuration);
    //     return source;
    // }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // ????????? ?????? h2 ??????, ????????? ?????? ??????
        return (web) -> web.ignoring().antMatchers("/h2-console/**"
                , "/favicon.ico"
                , "/error");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                // token??? ???????????? ???????????? ????????? csrf??? disable
                .csrf().disable()

                // // cors ?????? ??????
                .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)

                // ??????????????? ?????? ????????? ??????????????? ??????????????? ??????
                .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)

                // h2-console??? ?????? ?????? ??????
                // .and()
                // .headers()
                // .frameOptions()
                // .sameOrigin()

                // ????????? ??????????????? ??????????????? ????????? ??????
                // ????????? ???????????? ?????? ????????? STATELESS??? ??????
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                .and()
                .logout()
                .logoutUrl("/users/logout")
                // ?????????, ???????????? ??? ????????? ?????? ??? ????????? ???????????? API??? permitAll
                .and()
                .authorizeRequests()

                .antMatchers("/users/auth/renew").hasAnyAuthority(Authority.ROLE_USER.name())
                .antMatchers("/users/mypage").hasAnyAuthority(Authority.ROLE_USER.name())
                .antMatchers("/users/**").permitAll()
                .antMatchers("/").permitAll()

                .antMatchers(HttpMethod.GET, "/boards/**").permitAll()
                .antMatchers(HttpMethod.POST, "/boards/**").hasAnyAuthority(Authority.ROLE_USER.name())
                .antMatchers(HttpMethod.PUT, "/boards/**").hasAnyAuthority(Authority.ROLE_USER.name())
                .antMatchers(HttpMethod.DELETE, "/boards/**").hasAnyAuthority(Authority.ROLE_USER.name())

                // ???????????? ?????? ?????? ??????
                .anyRequest().authenticated()

                // JwtFilter ??? addFilterBefore ??? ???????????? JwtSecurityConfig ???????????? ??????
                .and()
                .apply(new JwtSecurityConfig(tokenProvider));

        return httpSecurity.build();
    }
}
