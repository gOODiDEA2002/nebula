package io.nebula.websocket.spring.session;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.security.Principal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringWebSocketSessionTest {

    @Test
    void usesHandshakeAttributeAsUserId() {
        org.springframework.web.socket.WebSocketSession nativeSession = nativeSession(
                Map.of("userId", "nebula-user"), null);

        SpringWebSocketSession session = new SpringWebSocketSession(nativeSession, JsonMapper.builder().build());

        assertThat(session.getUserId()).isEqualTo("nebula-user");
        assertThat(session.<String>getAttribute("userId")).isEqualTo("nebula-user");
    }

    @Test
    void authenticatedPrincipalTakesPrecedenceOverHandshakeAttribute() {
        Principal principal = () -> "authenticated-user";
        org.springframework.web.socket.WebSocketSession nativeSession = nativeSession(
                Map.of("userId", "query-user"), principal);

        SpringWebSocketSession session = new SpringWebSocketSession(nativeSession, JsonMapper.builder().build());

        assertThat(session.getUserId()).isEqualTo("authenticated-user");
    }

    private org.springframework.web.socket.WebSocketSession nativeSession(
            Map<String, Object> attributes, Principal principal) {
        org.springframework.web.socket.WebSocketSession session =
                mock(org.springframework.web.socket.WebSocketSession.class);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getPrincipal()).thenReturn(principal);
        return session;
    }
}
