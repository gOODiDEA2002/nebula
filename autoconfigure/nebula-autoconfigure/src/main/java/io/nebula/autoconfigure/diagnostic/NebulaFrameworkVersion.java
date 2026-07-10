package io.nebula.autoconfigure.diagnostic;

final class NebulaFrameworkVersion {

    private static final String DEVELOPMENT_VERSION = "2.1.0-SNAPSHOT";

    private NebulaFrameworkVersion() {
    }

    static String getVersion() {
        String implementationVersion = NebulaFrameworkVersion.class.getPackage().getImplementationVersion();
        return implementationVersion == null || implementationVersion.isBlank()
                ? DEVELOPMENT_VERSION
                : implementationVersion;
    }
}
