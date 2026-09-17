package com.curso.config;import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="jwt")
@org.springframework.validation.annotation.Validated
public record JwtProperties(@jakarta.validation.constraints.NotBlank String secret,@jakarta.validation.constraints.Positive long expirationSeconds){}
