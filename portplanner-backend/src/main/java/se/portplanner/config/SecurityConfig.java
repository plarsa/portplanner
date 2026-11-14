package se.portplanner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import se.portplanner.security.IamProperties;
import se.portplanner.security.UserDetailsServiceImpl;

import java.util.List;

@Configuration
@EnableConfigurationProperties(IamProperties.class)
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(10))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/version").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // Landing page and both SPA shells are public; each app signs the user
                // in through /api/auth and every /api endpoint is authorized on its own.
                .requestMatchers("/", "/index.html", "/*.ico", "/*.png", "/*.webmanifest").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/admin", "/admin/**").permitAll()
                .requestMatchers("/member", "/member/**").permitAll()
                // REN-14: rentals needs member:rw for booking, so the module level alone would
                // also open the admin API to members. This is the module's own role gate and is
                // deliberately not widened to the other modules here.
                .requestMatchers("/api/rentals/**").hasAnyRole("ADMIN", "HARBOUR_MASTER")
                .requestMatchers("/api/me/**").hasAnyRole("MEMBER", "ADMIN", "HARBOUR_MASTER")
                .anyRequest().authenticated()
            )
            .logout(l -> l
                .logoutUrl("/api/auth/logout")
                .deleteCookies("SESSION")
                .logoutSuccessHandler((req, res, authentication) -> res.setStatus(200)))
            .headers(h -> h.frameOptions(f -> f.sameOrigin()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174", "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
