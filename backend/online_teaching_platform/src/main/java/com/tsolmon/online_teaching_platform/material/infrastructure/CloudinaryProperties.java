package com.tsolmon.online_teaching_platform.material.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        String folder,
        String uploadPreset,
        /** Media Library folder for teacher profile avatars (Cloudinary asset folder; e.g. profiles). */
        String profileFolder,
        /** Upload preset for avatars; should match dashboard: folder profiles, unsigned/signed as configured. */
        String profileUploadPreset
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

    public String effectiveProfileFolder() {
        if (profileFolder == null || profileFolder.isBlank()) {
            return "profiles";
        }
        return profileFolder.trim().replaceAll("^/+|/+$", "");
    }

    /** Preset for profile images; falls back to global upload preset if unset. */
    public String effectiveProfileUploadPreset() {
        if (profileUploadPreset != null && !profileUploadPreset.isBlank()) {
            return profileUploadPreset.trim();
        }
        if (uploadPreset != null && !uploadPreset.isBlank()) {
            return uploadPreset.trim();
        }
        return "";
    }
}
