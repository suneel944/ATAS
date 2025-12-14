package com.atas.framework.config;

import com.atas.framework.security.CustomAuthenticationEntryPoint;
import com.atas.framework.security.CustomUserDetailsService;
import com.atas.framework.security.InternalApiAuthenticationFilter;
import com.atas.framework.security.JwtAuthenticationFilter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security configuration for JWT-based authentication. Configures security filter chain,
 * password encoding, CORS, and method-level security.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomUserDetailsService userDetailsService;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final InternalApiAuthenticationFilter internalApiAuthenticationFilter;
  private final CustomAuthenticationEntryPoint authenticationEntryPoint;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/auth/**")
                    .permitAll()
                    .requestMatchers("/api/v1/internal/auth/**")
                    .permitAll() // Token generation uses API key authentication
                    .requestMatchers("/api/v1/internal/**")
                    .hasAuthority("ROLE_INTERNAL_API") // Internal APIs require internal API token
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers("/actuator/info")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .requestMatchers("/login")
                    .permitAll()
                    .requestMatchers("/favicon.ico", "/favicon.*")
                    .permitAll()
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/static/**", "/webjars/**")
                    .permitAll()
                    .requestMatchers("/")
                    .permitAll()
                    .requestMatchers("/monitoring/dashboard")
                    .permitAll() // Allow page to load, auth checked in JavaScript
                    .requestMatchers("/monitoring/database")
                    .permitAll() // Allow page to load, auth checked in JavaScript
                    .requestMatchers("/monitoring/**")
                    .authenticated() // API endpoints still require auth
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint))
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(
            internalApiAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12); // BCrypt strength 12
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        List.of("http://localhost:3000", "http://localhost:8080")); // Configure as needed
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
