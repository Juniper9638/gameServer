package projectj.sm.gameserver.configuration.socket;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import projectj.sm.gameserver.RedisUtil;
import projectj.sm.gameserver.security.JwtAuthToken;
import projectj.sm.gameserver.security.JwtAuthTokenProvider;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Log
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {
    public static final String AUTHORIZATION_HEADER = "x-auth-token";
    private final JwtAuthTokenProvider tokenProvider;
    private final RedisUtil redisUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (message.getHeaders().get("simpMessageType").toString().contains("CONNECT")) {
            if (accessor.getFirstNativeHeader(AUTHORIZATION_HEADER) != null) {
                Optional<String> token = Optional.of(accessor.getFirstNativeHeader("x-auth-token"));
                if (token.isPresent()) {
                    JwtAuthToken jwtAuthToken = tokenProvider.convertAuthToken(token.get());
                    if (jwtAuthToken.validate()) {
                        Authentication authentication = tokenProvider.getAuthentication(jwtAuthToken);
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        JwtAuthToken toke = (JwtAuthToken) SecurityContextHolder.getContext().getAuthentication().getCredentials();
                        Claims claims = toke.getData();

                        redisUtil.setData(accessor.getSessionId(), claims.toString());
                    }
                }
            }
        }
        if (message.getHeaders().get("simpMessageType").toString().contains("SUBSCRIBE") ||
            message.getHeaders().get("simpMessageType").toString().contains("UNSUBSCRIBE")) {
            Set<String> keys = redisUtil.getFindKeys(accessor.getSessionId());
            for (String key : keys) {
                redisUtil.updateKey(key, Duration.ofHours(3));
            }
        }

        return message;
    }
}
