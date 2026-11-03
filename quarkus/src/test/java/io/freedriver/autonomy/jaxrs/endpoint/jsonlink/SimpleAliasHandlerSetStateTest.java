package io.freedriver.autonomy.jaxrs.endpoint.jsonlink;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import io.freedriver.autonomy.jaxrs.view.AliasView;
import io.freedriver.autonomy.service.SimpleAliasService;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.Test;

class SimpleAliasHandlerSetStateTest {

    @Test
    void anonymousSetStateIsRefused() throws Exception {
        SimpleAliasHandler handler = new SimpleAliasHandler();
        handler.securityContext = anonymous();
        SimpleAliasService service = mock(SimpleAliasService.class);
        handler.simpleAliasService = service;

        assertThrows(
                NotAuthorizedException.class,
                () -> handler.setState(UUID.randomUUID(), Map.of("pin", true)));
        verify(service, never()).setState(any(), any());
    }

    @Test
    void missingSecurityContextSetStateIsRefused() throws Exception {
        SimpleAliasHandler handler = new SimpleAliasHandler();
        SimpleAliasService service = mock(SimpleAliasService.class);
        handler.simpleAliasService = service;

        assertThrows(
                NotAuthorizedException.class,
                () -> handler.setState(UUID.randomUUID(), Map.of("pin", true)));
        verify(service, never()).setState(any(), any());
    }

    @Test
    void authenticatedSetStateWrites() throws Exception {
        SimpleAliasHandler handler = new SimpleAliasHandler();
        handler.securityContext = authenticated();
        SimpleAliasService service = mock(SimpleAliasService.class);
        handler.simpleAliasService = service;
        UUID board = UUID.randomUUID();
        Map<String, Boolean> desired = Map.of("pin", true);
        when(service.makeView(board)).thenReturn(mock(AliasView.class));

        handler.setState(board, desired);
        verify(service).setState(board, desired);
    }

    @Test
    void anonymousSetGroupStillWrites() throws Exception {
        SimpleAliasHandler handler = new SimpleAliasHandler();
        handler.securityContext = anonymous();
        SimpleAliasService service = mock(SimpleAliasService.class);
        handler.simpleAliasService = service;
        UUID board = UUID.randomUUID();
        when(service.makeView(board)).thenReturn(mock(AliasView.class));

        handler.setGroup(board, "g", true);
        verify(service).setGroup(board, "g", true);
    }

    private static SecurityContext anonymous() {
        SecurityContext context = mock(SecurityContext.class);
        when(context.getUserPrincipal()).thenReturn(null);
        return context;
    }

    private static SecurityContext authenticated() {
        SecurityContext context = mock(SecurityContext.class);
        when(context.getUserPrincipal()).thenReturn(() -> "caller");
        return context;
    }
}
