package com.shiyq.config;

import com.shiyq.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.servlet.MultipartConfigElement;

/**
 * WebMvc配置类
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    public WebMvcConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    /**
     * 配置上传文件的目录
     */
    @Value("${file.uploadFolder}")
    private String uploadFolder;
    @Value("${file.publicAssetFolder:illustrations/web-img/}")
    private String publicAssetFolder;
    @Value("${file.publicAssetAccessPath:/public-assets/**}")
    private String publicAssetAccessPath;
    @Value("${file.maxFileSize}")
    private String maxFileSize;
    @Value("${file.maxRequestSize}")
    private String maxRequestSize;

    /**
     * 解决跨域问题
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") //设置允许跨域的路径
                .allowedOriginPatterns("*") //设置允许跨域请求的域名
                // 设置允许的方法
                .allowedMethods("POST", "GET", "PUT", "OPTIONS", "DELETE")
                .maxAge(3600) //跨域允许时间
                .allowCredentials(true); //是否允许证书 不再默认开启
    }

    /**
     * 配置JWT登录拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",
                        "/illustration/getRandomArtwork",
                        "/media",
                        publicAssetAccessPath
                )  // 不拦截
        ;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * 配置文件上传路径和请文件大小限制
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setLocation(uploadFolder);
        factory.setMaxFileSize(DataSize.parse(maxFileSize));
        factory.setMaxRequestSize(DataSize.parse(maxRequestSize));
        return factory.createMultipartConfig();
    }

    /**
     * 只公开站点装饰素材。用户上传内容统一通过带短期签名的 /media 接口访问。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadRoot = Paths.get(uploadFolder).toAbsolutePath().normalize();
        Path publicAssetRoot = uploadRoot.resolve(publicAssetFolder).normalize();
        if (!publicAssetRoot.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Public asset folder must stay inside upload folder");
        }
        String publicAssetLocation = publicAssetRoot.toUri().toString();
        if (!publicAssetLocation.endsWith("/")) {
            publicAssetLocation += "/";
        }
        registry.addResourceHandler(publicAssetAccessPath)
                .addResourceLocations(publicAssetLocation);
    }
}
