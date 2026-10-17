package com.iortatechnxt.brokerverse.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.iortatechnxt.brokerverse.common.util.EmailAddresses;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailAddressesTest {

  @ParameterizedTest
  @ValueSource(strings = {"juan.delacruz@bdo.com.ph", "a@b.co", "ops+nb@broker.example"})
  void acceptsPlausibleAddresses(String address) {
    assertThat(EmailAddresses.isValid(address)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "no-at.example.com",
        "@example.com",
        "a@@example.com",
        "a@b@example.com",
        "a@example",
        "a@.example.com",
        "a@example.c",
        "a b@example.com",
        "a@example.com,b@example.com",
        "a;b@example.com"
      })
  void rejectsMalformedAddresses(String address) {
    assertThat(EmailAddresses.isValid(address)).isFalse();
  }

  @Test
  void rejectsNullAndOverlongAddresses() {
    assertThat(EmailAddresses.isValid(null)).isFalse();
    assertThat(EmailAddresses.isValid("a@" + "x".repeat(260) + ".com")).isFalse();
  }

  @Test
  void runsInLinearTimeOnHostileInput() {
    String hostile = "!@!." + "!.".repeat(100_000);
    assertTimeoutPreemptively(
        Duration.ofSeconds(1), () -> assertThat(EmailAddresses.isValid(hostile)).isFalse());
  }
}
