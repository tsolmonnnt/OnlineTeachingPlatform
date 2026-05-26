package com.tsolmon.online_teaching_platform.material.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryPropertiesTest {

    @Test
    void isConfiguredRequiresAllCredentialsToBePresentAndNonBlank() {
        assertThat(props(null, "key", "secret").isConfigured()).isFalse();
        assertThat(props(" ", "key", "secret").isConfigured()).isFalse();
        assertThat(props("cloud", null, "secret").isConfigured()).isFalse();
        assertThat(props("cloud", " ", "secret").isConfigured()).isFalse();
        assertThat(props("cloud", "key", null).isConfigured()).isFalse();
        assertThat(props("cloud", "key", " ").isConfigured()).isFalse();
        assertThat(props("cloud", "key", "secret").isConfigured()).isTrue();
    }

    @Test
    void effectiveFoldersDefaultTrimAndStripOuterSlashes() {
        assertThat(new CloudinaryProperties("c", "k", "s", null, null, null, null).effectiveFolder())
                .isEqualTo("subjectFiles");
        assertThat(new CloudinaryProperties("c", "k", "s", "  ", null, null, null).effectiveFolder())
                .isEqualTo("subjectFiles");
        assertThat(new CloudinaryProperties("c", "k", "s", "/course/files/", null, null, null).effectiveFolder())
                .isEqualTo("course/files");

        assertThat(new CloudinaryProperties("c", "k", "s", null, null, null, null).effectiveProfileFolder())
                .isEqualTo("profiles");
        assertThat(new CloudinaryProperties("c", "k", "s", null, null, "  ", null).effectiveProfileFolder())
                .isEqualTo("profiles");
        assertThat(new CloudinaryProperties("c", "k", "s", null, null, "/teacher/profiles/", null).effectiveProfileFolder())
                .isEqualTo("teacher/profiles");
    }

    @Test
    void effectiveProfileUploadPresetPrefersProfilePresetThenGlobalPresetThenBlank() {
        assertThat(new CloudinaryProperties("c", "k", "s", null, "global", null, " profile ").effectiveProfileUploadPreset())
                .isEqualTo("profile");
        assertThat(new CloudinaryProperties("c", "k", "s", null, " global ", null, " ").effectiveProfileUploadPreset())
                .isEqualTo("global");
        assertThat(new CloudinaryProperties("c", "k", "s", null, null, null, null).effectiveProfileUploadPreset())
                .isEmpty();
        assertThat(new CloudinaryProperties("c", "k", "s", null, " ", null, null).effectiveProfileUploadPreset())
                .isEmpty();
    }

    private static CloudinaryProperties props(String cloudName, String apiKey, String apiSecret) {
        return new CloudinaryProperties(cloudName, apiKey, apiSecret, null, null, null, null);
    }
}
