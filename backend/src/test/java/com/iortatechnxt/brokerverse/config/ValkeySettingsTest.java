package com.iortatechnxt.brokerverse.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * The Valkey connection settings of {@code application.yml}: the {@code BROKERVERSE_VALKEY_*}
 * variables are read first, the former {@code BROKERVERSE_REDIS_*} names still work, so an existing
 * deployment keeps running unchanged.
 */
class ValkeySettingsTest {

  @Test
  void theValkeyVariablesSetTheConnection() throws IOException {
    StandardEnvironment env =
        environment(
            Map.of(
                "BROKERVERSE_VALKEY_HOST", "valkey.internal",
                "BROKERVERSE_VALKEY_PORT", "6380",
                "BROKERVERSE_VALKEY_TLS", "false",
                "BROKERVERSE_VALKEY_ENABLED", "false",
                "BROKERVERSE_VALKEY_KEY_PREFIX", "uat:"));

    assertThat(env.getProperty("spring.data.redis.host")).isEqualTo("valkey.internal");
    assertThat(env.getProperty("spring.data.redis.port")).isEqualTo("6380");
    assertThat(env.getProperty("spring.data.redis.ssl.enabled")).isEqualTo("false");
    assertThat(env.getProperty("brokerverse.redis.enabled")).isEqualTo("false");
    assertThat(env.getProperty("brokerverse.redis.key-prefix")).isEqualTo("uat:");
    assertThat(env.getProperty("management.health.redis.enabled")).isEqualTo("false");
  }

  @Test
  void theFormerRedisVariablesAreStillRead() throws IOException {
    StandardEnvironment env =
        environment(
            Map.of(
                "BROKERVERSE_REDIS_HOST", "cache.internal",
                "BROKERVERSE_REDIS_PASSWORD", "auth-token"));

    assertThat(env.getProperty("spring.data.redis.host")).isEqualTo("cache.internal");
    assertThat(env.getProperty("spring.data.redis.password")).isEqualTo("auth-token");
  }

  @Test
  void theValkeyNameWinsOverTheFormerName() throws IOException {
    StandardEnvironment env =
        environment(
            Map.of(
                "BROKERVERSE_VALKEY_HOST", "valkey.internal",
                "BROKERVERSE_REDIS_HOST", "cache.internal"));

    assertThat(env.getProperty("spring.data.redis.host")).isEqualTo("valkey.internal");
  }

  @Test
  void withoutVariablesTheDefaultsApply() throws IOException {
    StandardEnvironment env = environment(Map.of());

    assertThat(env.getProperty("spring.data.redis.host")).isEqualTo("localhost");
    assertThat(env.getProperty("spring.data.redis.port")).isEqualTo("6379");
    assertThat(env.getProperty("spring.data.redis.ssl.enabled")).isEqualTo("true");
    assertThat(env.getProperty("brokerverse.redis.enabled")).isEqualTo("true");
    assertThat(env.getProperty("brokerverse.redis.key-prefix")).isEqualTo("bv:");
  }

  /** The first (profile-independent) document of application.yml over the given variables. */
  private static StandardEnvironment environment(Map<String, Object> variables) throws IOException {
    List<PropertySource<?>> documents =
        new YamlPropertySourceLoader()
            .load("application", new ClassPathResource("application.yml"));
    StandardEnvironment env = new StandardEnvironment();
    env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    env.getPropertySources().addFirst(new MapPropertySource("variables", variables));
    env.getPropertySources().addLast(documents.get(0));
    return env;
  }
}
