package com.tsolmon.online_teaching_platform.material.infrastructure;

import com.cloudinary.Cloudinary;
import com.cloudinary.Url;
import com.tsolmon.online_teaching_platform.material.domain.TeachingMaterial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CloudinaryUrlService {
    private static final String DEFAULT_RESOURCE_TYPE = "image";
    private static final String DEFAULT_DELIVERY_TYPE = "upload";
    private static final String AUTHENTICATED_DELIVERY_TYPE = "authenticated";
    private static final Pattern VERSION_PATTERN = Pattern.compile("/v(\\d+)/");

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    public String deliveryUrl(TeachingMaterial material) {
        if (material == null || material.getSecureUrl() == null || material.getSecureUrl().isBlank()) {
            return null;
        }
        if (!cloudinaryProperties.isConfigured()) {
            return ensurePdfExtension(material, material.getSecureUrl());
        }

        try {
            if (requiresSignedDelivery(material)) {
                return buildSignedUrl(material);
            }
            if (needsPdfExtensionInUrl(material)) {
                return buildPublicPdfUrl(material);
            }
            return material.getSecureUrl();
        } catch (RuntimeException ex) {
            return fallbackUrl(material);
        }
    }

    static boolean needsPdfExtensionInUrl(TeachingMaterial material) {
        if (!isPdfMaterial(material)) {
            return false;
        }
        // Always rebuild URL for raw PDFs to ensure .pdf extension is present
        return "raw".equals(effectiveResourceType(material));
    }

    private String buildPublicPdfUrl(TeachingMaterial material) {
        String publicId = material.getCloudinaryPublicId();

        // For raw resource type, Cloudinary serves the file by appending
        // the extension directly to the public_id in the URL.
        // We must include .pdf in the public_id we pass to generate().
        String publicIdWithExt = publicIdHasExtension(publicId, "pdf")
                ? publicId
                : publicId + ".pdf";

        Long version = effectiveVersion(material);
        String deliveryType = effectiveDeliveryType(material);

        Url urlBuilder = cloudinary.url()
                .secure(true)
                .signed(false)
                .resourceType("raw")
                .type(deliveryType);

        if (version != null) {
            urlBuilder.version(version.toString());
        }

        // Do NOT set format() for raw — format is part of the public_id extension
        return urlBuilder.generate(publicIdWithExt);
    }

    static boolean requiresSignedDelivery(TeachingMaterial material) {
        return AUTHENTICATED_DELIVERY_TYPE.equals(effectiveDeliveryType(material));
    }

    private String buildSignedUrl(TeachingMaterial material) {
        String resourceType = effectiveResourceType(material);
        String publicId = material.getCloudinaryPublicId();

        // Same fix: for raw PDFs, append .pdf to public_id
        String publicIdWithExt = ("raw".equals(resourceType) && isPdfMaterial(material)
                && !publicIdHasExtension(publicId, "pdf"))
                ? publicId + ".pdf"
                : publicId;

        Url urlBuilder = cloudinary.url()
                .secure(true)
                .signed(true)
                .resourceType(resourceType)
                .type(effectiveDeliveryType(material));

        Long version = effectiveVersion(material);
        if (version != null) {
            urlBuilder.version(version.toString());
        }

        // Only set format for non-raw resources
        if (!"raw".equals(resourceType)) {
            String format = formatForSigning(material, resourceType, publicId);
            if (format != null) {
                urlBuilder.format(format);
            }
        }

        return urlBuilder.generate(publicIdWithExt);
    }

    private static String fallbackUrl(TeachingMaterial material) {
        String stored = material.getSecureUrl();
        if (stored == null || stored.isBlank()) {
            return null;
        }
        return ensurePdfExtension(material, stored);
    }

    // Appends .pdf to a plain URL string if needed (no SDK involved)
    private static String ensurePdfExtension(TeachingMaterial material, String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        if (isPdfMaterial(material) && !url.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return url + ".pdf";
        }
        return url;
    }

    public Map<String, Object> destroyOptions(TeachingMaterial material) {
        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", effectiveResourceType(material));
        String deliveryType = effectiveDeliveryType(material);
        if (!DEFAULT_DELIVERY_TYPE.equals(deliveryType)) {
            options.put("type", deliveryType);
        }
        return options;
    }

    static boolean isPdfMaterial(TeachingMaterial material) {
        if (material == null) {
            return false;
        }
        String contentType = material.getContentType();
        if (contentType != null && "application/pdf".equalsIgnoreCase(contentType.trim())) {
            return true;
        }
        if (endsWithPdf(material.getSecureUrl())) {
            return true;
        }
        return endsWithPdf(material.getTitle());
    }

    private static boolean endsWithPdf(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).trim().endsWith(".pdf");
    }

    static boolean urlLooksLikePdf(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains(".pdf") || lower.endsWith("/pdf");
    }

    private static Long effectiveVersion(TeachingMaterial material) {
        if (material.getCloudinaryVersion() != null) {
            return material.getCloudinaryVersion();
        }
        return parseVersionFromUrl(material.getSecureUrl());
    }

    static Long parseVersionFromUrl(String secureUrl) {
        if (secureUrl == null) {
            return null;
        }
        Matcher matcher = VERSION_PATTERN.matcher(secureUrl);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String effectiveResourceType(TeachingMaterial material) {
        String secureUrl = material.getSecureUrl();
        if (secureUrl != null) {
            if (secureUrl.contains("/raw/")) return "raw";
            if (secureUrl.contains("/video/")) return "video";
            if (secureUrl.contains("/image/")) return "image";
        }
        if (material.getCloudinaryResourceType() != null && !material.getCloudinaryResourceType().isBlank()) {
            return material.getCloudinaryResourceType().trim();
        }
        String contentType = material.getContentType();
        if (contentType == null) return DEFAULT_RESOURCE_TYPE;
        if (contentType.startsWith("image/")) return "image";
        if (contentType.startsWith("video/")) return "video";
        return "raw";
    }

    private static String effectiveDeliveryType(TeachingMaterial material) {
        if (material.getCloudinaryDeliveryType() != null && !material.getCloudinaryDeliveryType().isBlank()) {
            return material.getCloudinaryDeliveryType().trim();
        }
        String secureUrl = material.getSecureUrl();
        if (secureUrl != null && secureUrl.contains("/authenticated/")) {
            return "authenticated";
        }
        return DEFAULT_DELIVERY_TYPE;
    }

    private static String formatForSigning(TeachingMaterial material, String resourceType, String publicId) {
        // Do not set format for raw resources — extension goes in public_id
        if ("raw".equals(resourceType)) {
            return null;
        }
        String contentType = material.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        int slash = contentType.indexOf('/');
        if (slash < 0 || slash == contentType.length() - 1) {
            return null;
        }
        String subtype = contentType.substring(slash + 1).trim().toLowerCase(Locale.ROOT);
        if ("jpeg".equals(subtype)) subtype = "jpg";
        if (!subtype.matches("[a-z0-9.+-]+")) return null;
        return publicIdHasExtension(publicId, subtype) ? null : subtype;
    }

    static boolean publicIdHasExtension(String publicId, String extension) {
        if (publicId == null || publicId.isBlank() || extension == null || extension.isBlank()) {
            return false;
        }
        String suffix = "." + extension.trim().toLowerCase(Locale.ROOT);
        return publicId.toLowerCase(Locale.ROOT).endsWith(suffix);
    }
}