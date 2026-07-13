package com.vyaparsetu.common.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

/**
 * Authenticates STOMP CONNECT frames using the bearer JWT and authorizes
 * SUBSCRIBE frames so a client can only subscribe to its own user-scoped topics.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String USER_TOPIC_PREFIX = "/topic/notifications/";

    private final JwtTokenProvider tokenProvider;

    public StompAuthChannelInterceptor(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }
            AppPrincipal principal = tokenProvider.parse(header.substring(7));
            List<SimpleGrantedAuthority> authorities = principal.roles().stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            accessor.setUser(authentication);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith(USER_TOPIC_PREFIX)) {
                AppPrincipal principal = currentPrincipal(accessor.getUser());
                String targetUserId = destination.substring(USER_TOPIC_PREFIX.length());
                boolean ownsTopic = String.valueOf(principal.userId()).equals(targetUserId);
                if (!ownsTopic && !principal.roles().contains("ADMIN")) {
                    throw new IllegalArgumentException("Cannot subscribe to another user's notifications");
                }
            }
        }
        return message;
    }

    private AppPrincipal currentPrincipal(Principal user) {
        if (user instanceof Authentication authn && authn.getPrincipal() instanceof AppPrincipal principal) {
            return principal;
        }
        throw new IllegalArgumentException("Not authenticated on this WebSocket session");
    }
}
