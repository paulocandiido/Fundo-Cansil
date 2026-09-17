package com.curso.config;
import java.time.*;import org.springframework.context.annotation.*;
@Configuration public class RelogioConfig{@Bean Clock relogio(){return Clock.system(ZoneId.of("America/Sao_Paulo"));}}
