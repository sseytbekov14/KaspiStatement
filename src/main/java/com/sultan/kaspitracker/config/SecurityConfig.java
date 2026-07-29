package com.sultan.kaspitracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.security.username}")
    private String username;

    @Value("${app.security.password}")
    private String password;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. CSRF: session-based (default HttpSessionCsrfTokenRepository).
            //    Thymeleaf reads the token from the session automatically via ${_csrf}.
            //    Disable CSRF for API endpoints (they use Basic Auth) and for /login
            //    to prevent 403 on mobile browsers that drop session cookies between
            //    GET /login and POST /login.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/login")
            )
            
            // 2. Authorize requests
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
                .anyRequest().authenticated()
            )
            
            // 3. Form login → always redirect to dashboard after success
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            
            // 4. Session management
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
            )
            
            // 5. Basic Auth for REST API / Swagger
            .httpBasic(Customizer.withDefaults())
            
            // 6. Logout: invalidate session, redirect to login
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        String finalPassword = password;
        
        if (finalPassword != null && !finalPassword.startsWith("{")) {
            if (finalPassword.startsWith("$2a$") || finalPassword.startsWith("$2b$") || finalPassword.startsWith("$2y$")) {
                finalPassword = "{bcrypt}" + finalPassword;
            } else {
                finalPassword = "{noop}" + finalPassword;
                log.warn("Using {noop} password encoder. Please use a BCrypt hash in production.");
            }
        }

        UserDetails user = User.builder()
            .username(username)
            .password(finalPassword)
            .roles("USER")
            .build();

        return new InMemoryUserDetailsManager(user) {
            @Override
            public UserDetails loadUserByUsername(String inputUsername) throws org.springframework.security.core.userdetails.UsernameNotFoundException {
                try {
                    return super.loadUserByUsername(inputUsername);
                } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
                    log.warn("Authentication failed: User '{}' not found", inputUsername);
                    throw ex;
                }
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
