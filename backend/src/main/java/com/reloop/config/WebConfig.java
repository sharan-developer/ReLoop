package com.reloop.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves locally stored PUBLIC images (product/repair/listing photos) when Cloudinary is not configured. */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AppProperties props;

    public WebConfig(AppProperties props) { this.props = props; }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path publicDir = Path.of(props.getUploadDir(), "public").toAbsolutePath();
        registry.addResourceHandler("/uploads/**").addResourceLocations(publicDir.toUri().toString());
    }
}
