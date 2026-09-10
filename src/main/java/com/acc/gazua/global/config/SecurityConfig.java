package com.acc.gazua.global.config;

import com.acc.gazua.global.security.exception.JwtAccessDeniedHandler;
import com.acc.gazua.global.security.exception.JwtAuthenticationEntryPoint;
import com.acc.gazua.global.security.filter.JwtFilter;
import com.acc.gazua.global.security.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final JwtProvider jwtProvider;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    @Order(1)           // 메인 체인보다 먼저 평가되어야 한다
    @Profile("dev")     // prod 에서는 빈 자체가 만들어지지 않는다
    SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http){
        http
                .securityMatcher(PathRequest.toH2Console()) // 이 체인은 H2 콘솔 경로만 담당
                .csrf(csrf -> csrf.disable())
                // H2 콘솔은 내부적으로 frame 을 쓰므로 이 체인에서만 허용한다.
                // H2 Console을 위한 Frame Options 설정 (H2 사용 시 필수)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http){

        http     //jwt 사용으로 비활성화
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                //ExceptionTranslationFilter 에서 사용
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/accommodation/*/like").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/accommodation/*/review").permitAll()
                        .requestMatchers("/api/accommodation/*/review", "/api/accommodation/*/review/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/accommodation").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,"/api/accommodation","/api/accommodation/**").permitAll()
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/logout",
                                "/api/auth/re-issue",
                                "/api/users/signup",
                                "/api/payment/webhooks"
                        ).permitAll()
                        //정적 자원 허용
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .anyRequest().authenticated())
        ;


        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET","POST","DELETE","PUT","PATCH","OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization","Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); //preflight 캐시

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**",configuration);
        return source;

    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
