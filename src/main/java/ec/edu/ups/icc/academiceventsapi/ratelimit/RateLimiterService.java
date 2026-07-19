package ec.edu.ups.icc.academiceventsapi.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Contador distribuido atómico sobre Redis (ventana fija): INCR + EXPIRE en un único
 * script Lua para evitar condiciones de carrera entre solicitudes concurrentes.
 */
@Component
public class RateLimiterService {

    private static final String SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if tonumber(current) == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            local ttl = redis.call('TTL', KEYS[1])
            return {current, ttl}
            """;

    private static final RedisScript<List> SCRIPT_INSTANCE = new DefaultRedisScript<>(SCRIPT, List.class);

    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("unchecked")
    public RateLimitResult tryConsume(String key, int limit, Duration window) {
        List<Long> result = (List<Long>) redisTemplate.execute(SCRIPT_INSTANCE, List.of(key),
                String.valueOf(window.toSeconds()));
        long current = result.get(0);
        long ttl = result.get(1);
        boolean allowed = current <= limit;
        return new RateLimitResult(allowed, ttl < 0 ? window.toSeconds() : ttl);
    }
}
