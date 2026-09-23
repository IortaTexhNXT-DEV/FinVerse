package com.iortatechnxt.finverse.support;

import com.iortatechnxt.finverse.party.api.dto.PartyRequest;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.domain.PartyType;
import com.iortatechnxt.finverse.party.service.PartyService;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Creates fresh, authorized parties for tests whose assertions depend on a party having no other
 * open items. Integration tests share one database and run in any order, so tests must not assume
 * that shared demo parties (C-000201, A-0002, ...) are untouched by other test classes.
 */
@Component
public class TestParties {

  private static final AtomicInteger SEQ = new AtomicInteger();

  private final PartyService parties;
  private final AsUser as;
  private final TestData data;

  TestParties(PartyService parties, AsUser as, TestData data) {
    this.parties = parties;
    this.as = as;
    this.data = data;
  }

  /**
   * Creates a party (maker: accountant) and authorizes it (checker).
   *
   * @param type party type
   * @return the active party
   */
  public Party create(PartyType type) {
    String code = "T" + SEQ.incrementAndGet() + "-" + System.nanoTime() % 1_000_000;
    Party draft =
        as.run(
            "accountant",
            () ->
                parties.create(
                    new PartyRequest(
                        data.company().getId(),
                        code,
                        "Test " + type + " " + code,
                        type,
                        null,
                        null,
                        null,
                        null,
                        "PHP",
                        30,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)));
    return as.run("checker", () -> parties.authorize(draft.getId()));
  }
}
