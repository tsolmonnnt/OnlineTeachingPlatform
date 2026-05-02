package com.tsolmon.online_teaching_platform.material.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudinary")
public record CloudinaryProperties(
        String cloudName ,
        String apiKey,
        String apiSecret,
        String folder,
        String uploadPreset
) {
    public boolean isConfigured() {
        return cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
    }

    public String effectiveFolder() {
        if (folder == null || folder.isBlank()) {
            return "subjectFiles";
        }
        return folder.trim().replaceAll("^/+|/+$", "");
    }
}
