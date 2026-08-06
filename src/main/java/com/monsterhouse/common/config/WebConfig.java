package com.monsterhouse.common.config;
import com.monsterhouse.common.ratelimit.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import java.nio.file.Paths;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer{
    private final CorsProperties corsProperties;
    private final MessageSource messageSource;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final StorageProperties storageProperties;

    /**
     * 공개 폼에만 겁니다. 관리자 로그인은 계정 잠금이 이미 막고 있고,
     * 거기에 IP 제한까지 걸면 운영자가 사무실 공용 IP 에서 함께 잠깁니다.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/bookings", "/api/inquiries");
    }

    /**
     * 로컬 저장소(app.storage.type=local)로 올린 이미지를 /uploads/** 로 서빙합니다.
     * 운영(S3)에서는 CloudFront 가 대신하므로 이 매핑을 타지 않습니다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry){
        if (storageProperties.isS3()) {
            return;
        }
        String location = Paths.get(storageProperties.localPath())
                .toAbsolutePath().normalize().toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
    @Bean
    @ConditionalOnMissingBean(name = "localeResolver")
    public LocaleResolver localeResolver(){
        return new ApiLocaleResolver();
    }
    @Bean
    public LocalValidatorFactoryBean validatorFactory(){
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
    @Override
    public Validator getValidator(){
        return validatorFactory();
    }
    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/api/**")
                // allowedOrigins 가 아니라 allowedOriginPatterns 인 이유:
                //   전자는 정확히 일치하는 문자열만 허용해서 "http://localhost:*" 같은 패턴을 못 씁니다.
                //   로컬 dev 서버 포트가 유동적이면(5173 점유 시 자동 배정) 매번 CORS 로 막힙니다.
                //   패턴 방식은 정확한 문자열도 그대로 받으므로 운영 설정은 바뀌지 않습니다.
                //   (allowCredentials(true) 와 함께 "*" 를 쓰려면 반드시 이쪽이어야 합니다)
                .allowedOriginPatterns(corsProperties.allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
