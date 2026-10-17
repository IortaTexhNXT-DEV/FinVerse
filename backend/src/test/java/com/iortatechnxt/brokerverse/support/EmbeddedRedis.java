package com.iortatechnxt.brokerverse.support;

import com.github.fppt.jedismock.RedisServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * One in-JVM Redis protocol server (jedis-mock, pure Java: no Docker, no native binary) shared by
 * the tests of the JVM, and Lettuce connections to it.
 */
public final class EmbeddedRedis {

  private static RedisServer server;

  private EmbeddedRedis() {}

  /**
   * The running server, started on first use.
   *
   * @return server
   */
  public static synchronized RedisServer server() {
    if (server == null) {
      try {
        server = RedisServer.newRedisServer().start();
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }
    return server;
  }

  /**
   * Port of the server.
   *
   * @return port
   */
  public static int port() {
    return server().getBindPort();
  }

  /**
   * A new connection factory (one per simulated application instance).
   *
   * @return started connection factory
   */
  public static LettuceConnectionFactory connectionFactory() {
    LettuceConnectionFactory factory =
        new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", port()));
    factory.afterPropertiesSet();
    factory.start();
    return factory;
  }

  /**
   * A template on a new connection.
   *
   * @return template
   */
  public static StringRedisTemplate template() {
    StringRedisTemplate template = new StringRedisTemplate(connectionFactory());
    template.afterPropertiesSet();
    return template;
  }
}
