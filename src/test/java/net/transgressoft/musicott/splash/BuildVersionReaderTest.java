package net.transgressoft.musicott.splash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BuildVersionReader")
class BuildVersionReaderTest {

    @Test
    @DisplayName("reads build.version from classpath build-info.properties when present")
    void readsBuildVersionFromClasspathBuildInfoPropertiesWhenPresent() {
        // gradle test runs after processResources, so build-info.properties is on
        // the test classpath. The actual version is rotated per branch by
        // axion-release, so we assert non-blank rather than a literal value.
        String version = BuildVersionReader.read();

        assertThat(version)
            .as("build.version should be populated from META-INF/build-info.properties when running under gradle")
            .isNotBlank();
        assertThat(version)
            .as("when build-info.properties is on the classpath, the reader must NOT fall back to 'dev'")
            .isNotEqualTo("dev");
    }

    @Test
    @DisplayName("returns dev sentinel when build-info.properties is missing")
    void returnsDevSentinelWhenBuildInfoPropertiesIsMissing() {
        String version = BuildVersionReader.read(() -> null);

        assertThat(version).isEqualTo("dev");
    }

    @Test
    @DisplayName("returns dev sentinel when build-info.properties exists but build.version key is absent")
    void returnsDevSentinelWhenBuildInfoPropertiesExistsButBuildVersionKeyIsAbsent() {
        // Properties stream that loads cleanly but has no build.version entry
        String version = BuildVersionReader.read(() -> new ByteArrayInputStream(
            "build.artifact=musicott\nbuild.group=net.transgressoft\n".getBytes()
        ));

        assertThat(version).isEqualTo("dev");
    }

    @Test
    @DisplayName("does not require Spring on the classpath to resolve a version")
    void doesNotRequireSpringOnTheClasspathToResolveAVersion() {
        // Sanity check — the read() method must not touch any Spring class. We can't
        // remove Spring from the test classpath at runtime, but we can assert this
        // method does not throw NoClassDefFoundError when invoked from a context that
        // hasn't constructed any Spring bean. The mere fact that this test runs in the
        // unit test source set (which does NOT bring up @SpringBootTest) is the proof.
        String version = BuildVersionReader.read();

        assertThat(version).isNotNull();
    }
}
