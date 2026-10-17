package com.iortatechnxt.brokerverse.submitted.service.adapter;

import com.iortatechnxt.brokerverse.submitted.service.port.SubmittedSourceFeed;
import java.time.LocalDate;
import java.util.List;

/** Default {@link SubmittedSourceFeed} while no bank feed is connected (SP SQ01): nothing. */
public class NoSourceFeed implements SubmittedSourceFeed {

  @Override
  public List<FeedFile> pull(Long companyId, String sourceCode, LocalDate businessDate) {
    return List.of();
  }
}
