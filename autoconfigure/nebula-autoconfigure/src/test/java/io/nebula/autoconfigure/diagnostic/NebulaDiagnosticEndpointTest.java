package io.nebula.autoconfigure.diagnostic;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NebulaDiagnosticEndpointTest {

    @Test
    void diagnosticDefaultsMatchAutoConfigurationPolicy() {
        var endpoint = new NebulaDiagnosticEndpoint.NebulaDiagnosticEndpointBean(
                new MockEnvironment(), new StaticApplicationContext());

        Map<String, Object> diagnostic = endpoint.diagnostic();

        assertThat(map(diagnostic.get("framework")))
                .containsEntry("version", "2.1.0-SNAPSHOT");
        assertThat(map(diagnostic.get("discovery")))
                .containsEntry("enabled", false)
                .containsEntry("status", "DISABLED");
        assertThat(map(map(diagnostic.get("rpc")).get("http")))
                .containsEntry("enabled", false);
        assertThat(map(map(diagnostic.get("rpc")).get("discoveryIntegration")))
                .containsEntry("enabled", false);
        assertThat(map(diagnostic.get("asyncRpc")))
                .containsEntry("enabled", false);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
