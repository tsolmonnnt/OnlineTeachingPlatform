package com.tsolmon.online_teaching_platform.material.infrastructure;

import com.cloudinary.Cloudinary;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties props) {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", props.cloudName() != null ? props.cloudName() : "");
        config.put("api_key", props.apiKey() != null ? props.apiKey() : "");
        config.put("api_secret", props.apiSecret() != null ? props.apiSecret() : "");
        return new Cloudinary(config);
    }
}
