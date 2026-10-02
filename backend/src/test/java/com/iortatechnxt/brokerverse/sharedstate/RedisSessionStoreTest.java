package com.iortatechnxt.brokerverse.sharedstate;

import com.iortatechnxt.brokerverse.cache.service.RedisSettings;
import com.iortatechnxt.brokerverse.security.service.SharedCounterStore;
import com.iortatechnxt.brokerverse.security.service.TokenRevocationStore;
import com.iortatechnxt.brokerverse.sharedstate.service.RedisSessionStore;
import com.iortatechnxt.brokerverse.support.EmbeddedRedis;
import java.time.Clock;

/** The Redis session store against the in-JVM Redis server. */
class RedisSessionStoreTest extends SessionStoreContract {

  private static final RedisSettings SETTINGS = new RedisSettings(true, "test:");
  private final RedisSessionStore store =
      new RedisSessionStore(EmbeddedRedis.template(), SETTINGS, Clock.systemUTC());
  private final RedisSessionStore otherInstance =
      new RedisSessionStore(EmbeddedRedis.template(), SETTINGS, Clock.systemUTC());

  @Override
  SharedCounterStore counters() {
    return store;
  }

  @Override
  SharedCounterStore otherInstanceCounters() {
    return otherInstance;
  }

  @Override
  TokenRevocationStore revocations() {
    return store;
  }
}
