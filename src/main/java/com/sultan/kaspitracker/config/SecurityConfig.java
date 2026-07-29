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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

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
        // Use a non-deferred handler so the CSRF token is eagerly resolved
        // and always available as a request attribute for Thymeleaf forms.
        // This fixes the 403 on mobile Safari where deferred tokens cause a mismatch.
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
            // 1. CSRF: cookie-based repository + eager token loading
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(requestHandler)
            )
            
            // 2. Authorize requests — explicitly permit /login and static resources
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
                .anyRequest().authenticated()
            )
            
            // 3. Form login with explicit redirect to dashboard
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
                .maximumSessions(1)
            )
            
            // 5. Basic Auth for API / Swagger
            .httpBasic(Customizer.withDefaults())
            
            // 6. Logout: clear session and cookies, redirect to login
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
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
