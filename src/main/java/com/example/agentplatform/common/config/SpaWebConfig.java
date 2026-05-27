package com.example.agentplatform.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        if (resourcePath.startsWith("api/")
                                || resourcePath.startsWith("/api/")
                                || resourcePath.startsWith("api")) {
                            return null;
                        }
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (resourcePath.endsWith(".js")
                                || resourcePath.endsWith(".css")
                                || resourcePath.endsWith(".svg")
                                || resourcePath.endsWith(".png")
                                || resourcePath.endsWith(".jpg")
                                || resourcePath.endsWith(".jpeg")
                                || resourcePath.endsWith(".gif")
                                || resourcePath.endsWith(".ico")
                                || resourcePath.endsWith(".woff")
                                || resourcePath.endsWith(".woff2")
                                || resourcePath.endsWith(".ttf")
                                || resourcePath.endsWith(".eot")) {
                            return null;
                        }
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
