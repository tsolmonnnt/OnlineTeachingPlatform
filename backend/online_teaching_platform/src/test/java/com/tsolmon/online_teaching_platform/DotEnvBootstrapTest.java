package com.tsolmon.online_teaching_platform;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DotEnvBootstrapTest {

    @Test
    void loadUsesFirstExistingCandidateWithoutOverridingEnvironmentOrSystemProperties() throws Exception {
        Path tempRoot = Files.createTempDirectory("dotenv-bootstrap-test");
        Path nestedDirectory = tempRoot.resolve("online_teaching_platform");
        Path envFile = nestedDirectory.resolve(".env");
        String newKey = "DOTENV_BOOTSTRAP_TEST_NEW";
        String existingKey = "DOTENV_BOOTSTRAP_TEST_EXISTING";

        Files.createDirectories(nestedDirectory);
        Files.writeString(envFile, """
                DOTENV_BOOTSTRAP_TEST_NEW=from-file
                DOTENV_BOOTSTRAP_TEST_EXISTING=from-file
                PATH=from-file
                """);
        System.clearProperty(newKey);
        System.clearProperty("PATH");
        System.setProperty(existingKey, "from-system-property");

        try {
            DotEnvBootstrap.load(tempRoot);

            assertThat(System.getProperty(newKey)).isEqualTo("from-file");
            assertThat(System.getProperty(existingKey)).isEqualTo("from-system-property");
            assertThat(System.getProperty("PATH")).isNull();
        } finally {
            System.clearProperty(newKey);
            System.clearProperty(existingKey);
            Files.deleteIfExists(envFile);
            Files.deleteIfExists(nestedDirectory);
            Files.deleteIfExists(tempRoot);
        }
    }
}
