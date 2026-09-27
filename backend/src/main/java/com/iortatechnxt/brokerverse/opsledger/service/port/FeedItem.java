package com.iortatechnxt.brokerverse.opsledger.service.port;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One record exchanged with an external system through a feed port (BRQID.004): a business key and
 * its fields by name. Field names follow the BRD lists until the interface specifications are known
 * (OQ01).
 *
 * @param key business key (idempotency key of the record)
 * @param fields values by field name, in order
 */
public record FeedItem(String key, Map<String, String> fields) {

  /** Ordered defensive copy. */
  public FeedItem {
    fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
  }
}
