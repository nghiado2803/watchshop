package com.example.watchshop.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // --- 1. Cấu hình Đa ngôn ngữ (Locale) ---
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("lang");
        resolver.setDefaultLocale(Locale.forLanguageTag("vi"));
        resolver.setCookieMaxAge(Duration.ofDays(30));
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");  // ?lang=vi hoặc ?lang=en
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    // --- 2. Cấu hình đường dẫn hiển thị ảnh ---
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map đường dẫn "/images/**" trên trình duyệt -> vào thư mục "uploads" thực tế trên máy
        String uploadPath = Paths.get("uploads").toFile().getAbsolutePath();

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:/" + uploadPath + "/");
    }

    // --- 3. [FIX LỖI QUAN TRỌNG] Cấu hình Tomcat bỏ giới hạn Upload ---
    // Đoạn này xử lý lỗi: FileCountLimitExceededException
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers((Connector connector) -> {
            // Cho phép upload số lượng file không giới hạn (-1) thay vì giới hạn mặc định
            connector.setProperty("maxParameterCount", "-1");

            // Cho phép dung lượng file lớn (Ví dụ: 100MB = 104857600 bytes)
            connector.setProperty("maxPostSize", "104857600");

            // Cho phép "nuốt" dữ liệu lớn nếu upload bị ngắt giữa chừng
            connector.setProperty("maxSwallowSize", "-1");
        });
    }
}