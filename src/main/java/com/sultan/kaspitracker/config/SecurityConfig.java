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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
            // 1. Disable CSRF for API endpoints
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            
            // 2. Authorize requests
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/webjars/**", "/favicon.ico").permitAll()
                .anyRequest().authenticated()
            )
            
            // 3. Enable form login for Web UI
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            
            // 4. Session management to prevent invalid CSRF token exceptions on expired sessions
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
                .maximumSessions(1)
            )
            
            // 5. Enable Basic Auth for API / Swagger
            .httpBasic(Customizer.withDefaults())
            
            // 6. Enable default logout
            .logout(Customizer.withDefaults());

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
