package com.sigi.configuration.websocket;

import com.sigi.configuration.security.jwt.JwtUtil;
import com.sigi.services.service.auth.AuthUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AuthUserService userService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/chat");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
                new ChannelInterceptor() {
                    @Override
                    public Message<?> preSend(Message<?> message, MessageChannel channel) {
                        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                            String tokenHeader = accessor.getFirstNativeHeader("Authorization");
                            if (tokenHeader != null && !tokenHeader.isBlank()) {
                                String token = tokenHeader.startsWith("Bearer ") ? tokenHeader.substring(7) : tokenHeader;
                                try {
                                    String username = jwtUtil.extractUserName(token);
                                    if (username != null && !username.isBlank()) {
                                        UserDetails user = userService.loadUserByUsername(username);
                                        if (jwtUtil.validateToken(token, user)) {
                                            Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                                            accessor.setUser(auth);
                                        } else {
                                            log.warn("Token inválido en handshake WebSocket para username: {}", username);
                                        }
                                    } else {
                                        log.warn("No se pudo extraer username del token en handshake WebSocket");
                                    }
                                } catch (Exception e) {
                                    log.error("❌ Error al autenticar WebSocket: {}", e.getMessage());
                                }
                            } else {
                                log.warn("No se recibió header Authorization en handshake WebSocket");
                            }
                        }
                        return message;
                    }
                },
                new SecurityContextChannelInterceptor()
        );
    }
}