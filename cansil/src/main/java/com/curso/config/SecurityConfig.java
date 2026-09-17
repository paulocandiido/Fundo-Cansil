package com.curso.config;
import com.curso.repositories.UsuarioRepository;import com.curso.security.JwtFilter;import org.springframework.boot.context.properties.EnableConfigurationProperties;import org.springframework.context.annotation.*;import org.springframework.http.HttpStatus;import org.springframework.security.config.annotation.web.builders.HttpSecurity;import org.springframework.security.config.http.SessionCreationPolicy;import org.springframework.security.core.userdetails.*;import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.security.web.SecurityFilterChain;import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration @EnableConfigurationProperties(JwtProperties.class) public class SecurityConfig{
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean UserDetailsService userDetailsService(UsuarioRepository r){return email->{var u=r.findByEmailIgnoreCase(email).orElseThrow(()->new UsernameNotFoundException("Credenciais inválidas"));return User.withUsername(u.getEmail()).password(u.getSenhaHash()).roles("USER").build();};}
 @Bean SecurityFilterChain chain(HttpSecurity h, JwtFilter f, com.fasterxml.jackson.databind.ObjectMapper mapper) throws Exception {
  return h.cors(org.springframework.security.config.Customizer.withDefaults())
   .csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .exceptionHandling(e -> e
    .authenticationEntryPoint((req, res, ex) -> writeError(mapper, req, res, HttpStatus.UNAUTHORIZED, "Não autenticado"))
    .accessDeniedHandler((req, res, ex) -> writeError(mapper, req, res, HttpStatus.FORBIDDEN, "Acesso negado")))
   .authorizeHttpRequests(a -> a.requestMatchers("/api/auth/**", "/h2-console/**", "/api/grupoproduto/**", "/api/produto/**").permitAll().anyRequest().authenticated())
   .headers(x -> x.frameOptions(y -> y.sameOrigin()))
   .addFilterBefore(f, UsernamePasswordAuthenticationFilter.class).build();
 }
 private static void writeError(com.fasterxml.jackson.databind.ObjectMapper mapper,
   jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response,
   HttpStatus status, String message) throws java.io.IOException {
  response.setStatus(status.value());
  response.setContentType("application/json");
  response.setCharacterEncoding("UTF-8");
  mapper.writeValue(response.getWriter(), new com.curso.resources.exceptions.StandardError(
   System.currentTimeMillis(), status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
 }
}
