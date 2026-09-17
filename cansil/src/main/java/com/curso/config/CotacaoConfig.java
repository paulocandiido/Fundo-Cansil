package com.curso.config;
import org.springframework.boot.context.properties.EnableConfigurationProperties; import org.springframework.context.annotation.Configuration;
@Configuration @EnableConfigurationProperties(CotacaoProperties.class) public class CotacaoConfig {
 @org.springframework.context.annotation.Bean
 org.springframework.boot.web.client.RestClientCustomizer cotacaoTimeouts(){return builder->{var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(3000);factory.setReadTimeout(5000);builder.requestFactory(factory);};}
}
