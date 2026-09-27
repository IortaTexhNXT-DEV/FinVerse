package com.iortatechnxt.brokerverse.sharedstate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.security.service.AppUserDetailsService;
import com.iortatechnxt.brokerverse.security.service.JwtTokenService;
import com.iortatechnxt.brokerverse.sharedstate.service.RedisJobLock;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.PlatformServicesSupport;
import com.iortatechnxt.brokerverse.system.domain.JobRun;
import com.iortatechnxt.brokerverse.system.domain.JobRunStatus;
import com.iortatechnxt.brokerverse.system.domain.JobTrigger;
import com.iortatechnxt.brokerverse.system.service.JobLock;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.JobRunService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The application with Redis enabled: reference data cached on Redis and evicted on write, job runs
 * locked on Redis, the token denylist and the failed-login counter on Redis.
 */
class RedisEnabledPlatformIT extends PlatformServicesSupport {

  @Autowired private StringRedisTemplate redis;
  @Autowired private JobLock jobLock;
  @Autowired private JobRunService jobRuns;
  @Autowired private SystemParameterService parameters;
  @Autowired private AppUserDetailsService users;
  @Autowired private JwtTokenService tokens;
  @Autowired private MockMvc mvc;
  @Autowired private AsUser as;
  @Autowired private Api api;

  @Test
  void theRedisImplementationsAreActive() throws Exception {
    assertThat(jobLock).isInstanceOf(RedisJobLock.class);
    api.doGet("admin", "/api/v1/admin/caches")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].store").value("REDIS"));
  }

  @Test
  void aParameterWriteEvictsTheRedisEntryOnEveryInstance() {
    String key = SystemParameterService.JOB_HISTORY_DAYS;
    String redisKey = "it:cache:system-parameters::" + key;
    String before = parameters.text(key, "90");
    assertThat(redis.hasKey(redisKey)).isTrue();
    try {
      as.run("admin", () -> parameters.update(key, "77"));
      assertThat(redis.hasKey(redisKey)).isFalse();
      assertThat(parameters.intValue(key, 0)).isEqualTo(77);
      assertThat(redis.opsForValue().get(redisKey)).contains("77");
    } finally {
      as.run("admin", () -> parameters.update(key, before));
    }
    assertThat(users.loadUserByUsername("auditor").getAuthorities()).isNotEmpty();
    assertThat(redis.hasKey("it:cache:security-role-permissions::AUDITOR")).isTrue();
  }

  @Test
  void ofTwoConcurrentRunsOneRunsAndOneIsSkipped() throws Exception {
    String job = "REDIS_CONCURRENT_" + UUID.randomUUID().toString().substring(0, 8);
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CompletableFuture<JobRun> first =
        CompletableFuture.supplyAsync(
            () ->
                jobRuns.execute(
                    job,
                    JobTrigger.SCHEDULED,
                    () -> {
                      started.countDown();
                      try {
                        release.await(30, TimeUnit.SECONDS);
                      } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                      }
                      return new JobOutcome(1, "done");
                    }));
    assertThat(started.await(30, TimeUnit.SECONDS)).isTrue();
    assertThat(redis.hasKey("it:joblock:" + job)).isTrue();
    assertThat(
            jobRuns.execute(job, JobTrigger.SCHEDULED, () -> new JobOutcome(1, "no")).getStatus())
        .isEqualTo(JobRunStatus.SKIPPED_LOCKED);
    release.countDown();
    assertThat(first.get(30, TimeUnit.SECONDS).getStatus()).isEqualTo(JobRunStatus.SUCCEEDED);
    assertThat(redis.hasKey("it:joblock:" + job)).isFalse();
  }

  @Test
  void logoutRevokesTheTokenInRedisAndFailuresAreCountedThere() throws Exception {
    login("auditor", "wrong-password", 401);
    assertThat(redis.opsForValue().get("it:counter:login-failures:auditor")).isEqualTo("1");
    String token = JsonPath.read(login("auditor", "Brokerverse@2026", 200), "$.accessToken");
    assertThat(redis.hasKey("it:counter:login-failures:auditor")).isFalse();

    mvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNoContent());
    String revokedKey = "it:session:revoked:" + tokens.parse(token).orElseThrow().tokenId();
    assertThat(redis.hasKey(revokedKey)).isTrue();
    assertThat(redis.getExpire(revokedKey)).isPositive();
    mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  private String login(String username, String password, int expected) throws Exception {
    return mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().is(expected))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }
}
