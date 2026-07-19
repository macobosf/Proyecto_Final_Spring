package ec.edu.ups.icc.academiceventsapi.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Bloqueo temporal tras varios intentos fallidos de login, independiente del
 * límite general de solicitudes. Claves con prefijo `failed-login:` y `blocked-user:`.
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private static final String SCRIPT = """
            local attempts = redis.call('INCR', KEYS[1])
            if tonumber(attempts) == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            if tonumber(attempts) >= tonumber(ARGV[2]) then
                redis.call('SET', KEYS[2], '1', 'EX', ARGV[1])
            end
            return attempts
            """;

    private static final RedisScript<Long> SCRIPT_INSTANCE = new DefaultRedisScript<>(SCRIPT, Long.class);

    private final StringRedisTemplate redisTemplate;

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isBlocked(String identifier) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(blockedKey(identifier)));
    }

    public long getBlockRemainingSeconds(String identifier) {
        Long ttl = redisTemplate.getExpire(blockedKey(identifier), TimeUnit.SECONDS);
        return (ttl == null || ttl < 0) ? BLOCK_DURATION.toSeconds() : ttl;
    }

    public void registerFailure(String identifier) {
        redisTemplate.execute(SCRIPT_INSTANCE, List.of(failuresKey(identifier), blockedKey(identifier)),
                String.valueOf(BLOCK_DURATION.toSeconds()), String.valueOf(MAX_ATTEMPTS));
    }

    public void registerSuccess(String identifier) {
        redisTemplate.delete(failuresKey(identifier));
        redisTemplate.delete(blockedKey(identifier));
    }

    private String failuresKey(String identifier) {
        return "failed-login:" + identifier;
    }

    private String blockedKey(String identifier) {
        return "blocked-user:" + identifier;
    }
}
