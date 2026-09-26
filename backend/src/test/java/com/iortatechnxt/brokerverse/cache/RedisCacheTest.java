package com.iortatechnxt.brokerverse.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iortatechnxt.brokerverse.cache.service.CacheConfiguration;
import com.iortatechnxt.brokerverse.cache.service.CacheProperties;
import com.iortatechnxt.brokerverse.cache.service.CacheSpec;
import com.iortatechnxt.brokerverse.cache.service.RedisSettings;
import com.iortatechnxt.brokerverse.cache.service.WriteTransactionBypassCache;
import com.iortatechnxt.brokerverse.organization.service.OrganizationDirectory.CompanyRef;
import com.iortatechnxt.brokerverse.support.EmbeddedRedis;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/** The Redis cache manager: JSON values, per-cache time to live, clear, unknown names. */
class RedisCacheTest {

  private final LettuceConnectionFactory connections = EmbeddedRedis.connectionFactory();
  private final StringRedisTemplate redis = EmbeddedRedis.template();

  private CacheManager manager(String prefix) {
    StaticListableBeanFactory beans =
        new StaticListableBeanFactory(Map.of("redisConnectionFactory", connections));
    return new CacheConfiguration()
        .cacheManager(
            List.of(
                CacheSpec.of("test-units", Duration.ofHours(1)),
                CacheSpec.of("test-views", Duration.ofMinutes(5))
                    .servedOnlyOutsideWriteTransactions()),
            new CacheProperties(Map.of("test-units", Duration.ofMinutes(10)), null),
            new RedisSettings(true, prefix),
            beans.getBeanProvider(RedisConnectionFactory.class),
            new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
  }

  @Test
  void valuesAreStoredAsJsonWithTheirTimeToLiveAndCleared() {
    CacheManager manager = manager("c1:");
    Cache units = manager.getCache("test-units");
    CompanyRef company = new CompanyRef(7L, "FVI", "First Venture", "PHP", true);
    units.put("company:7", company);
    units.put("missing", null);

    CacheManager otherInstance = manager("c1:");
    assertThat(otherInstance.getCache("test-units").get("company:7", CompanyRef.class))
        .isEqualTo(company);
    assertThat(otherInstance.getCache("test-units").get("missing")).isNotNull();
    assertThat(redis.opsForValue().get("c1:cache:test-units::company:7")).contains("First Venture");
    Long ttl = redis.getExpire("c1:cache:test-units::company:7");
    assertThat(ttl).isBetween(500L, 600L);

    otherInstance.getCache("test-units").clear();
    assertThat(units.get("company:7")).isNull();
    assertThat(manager.getCache("test-views")).isInstanceOf(WriteTransactionBypassCache.class);
    assertThat(manager.getCache("no-such-cache")).isNull();
  }

  @Test
  void aCacheDeclaredTwiceIsRefused() {
    assertThatThrownBy(
            () ->
                new CacheConfiguration()
                    .cacheManager(
                        List.of(
                            CacheSpec.of("dup", Duration.ofMinutes(1)),
                            CacheSpec.of("dup", Duration.ofMinutes(1))),
                        new CacheProperties(null, null),
                        new RedisSettings(false, null),
                        new StaticListableBeanFactory()
                            .getBeanProvider(RedisConnectionFactory.class),
                        new ObjectMapper()))
        .isInstanceOf(IllegalStateException.class);
  }
}
