package com.iortatechnxt.brokerverse.opsledger.service.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.opsledger.service.port.FeedItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** CSV of the outbound Collection items. */
class ManualCollectionFeedTest {

  @Test
  void itemsBecomeACsvWithEveryFieldQuotedWhenNeeded() {
    Map<String, String> first = new LinkedHashMap<>();
    first.put("invoice", "BI-1");
    first.put("reason", "Rejected, \"late\"");
    String csv =
        ManualCollectionFeed.csv(
            List.of(new FeedItem("K1", first), new FeedItem("K2", Map.of("extra", "x"))));
    assertThat(csv.split("\n"))
        .containsExactly(
            "key,invoice,reason,extra", "K1,BI-1,\"Rejected, \"\"late\"\"\",", "K2,,,x");
  }
}
