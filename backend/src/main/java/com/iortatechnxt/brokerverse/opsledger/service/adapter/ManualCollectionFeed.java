package com.iortatechnxt.brokerverse.opsledger.service.adapter;

import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.Transport;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.Trigger;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun.FileRef;
import com.iortatechnxt.brokerverse.opsledger.service.ExtractRepositoryService;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInService;
import com.iortatechnxt.brokerverse.opsledger.service.port.CollectionFeed;
import com.iortatechnxt.brokerverse.opsledger.service.port.FeedItem;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DropContent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Default {@link CollectionFeed} (manual transport, OQ01/OQ45): nothing is pulled - Collection data
 * is entered or uploaded on the Operations screens - and outbound items are logged as a flow-in run
 * of the feed and written as a CSV file to the extract repository folder {@code COLLECTION/<feed>}
 * for the Collection team to pick up.
 */
public class ManualCollectionFeed implements CollectionFeed {

  private static final String CSV = "text/csv";

  private final FlowInService flowIn;
  private final ExtractRepositoryService repository;

  /**
   * Creates the adapter.
   *
   * @param flowIn flow-in runs
   * @param repository extract repository
   */
  public ManualCollectionFeed(FlowInService flowIn, ExtractRepositoryService repository) {
    this.flowIn = flowIn;
    this.repository = repository;
  }

  @Override
  public String transport() {
    return Transport.MANUAL_UPLOAD.name();
  }

  @Override
  public List<FeedItem> pending(Long companyId, String feedCode) {
    return List.of();
  }

  @Override
  public String send(Long companyId, String feedCode, List<FeedItem> items) {
    List<FeedItem> sent = new ArrayList<>();
    FlowInRun run =
        flowIn.run(
            feedCode,
            Trigger.MANUAL,
            FileRef.NONE,
            ctx -> {
              for (FeedItem item : items) {
                if (ctx.accept(item.key(), item.fields().toString(), item::key)) {
                  sent.add(item);
                }
              }
              if (!sent.isEmpty()) {
                repository.store(
                    companyId,
                    new ExtractFile.Location("COLLECTION/" + feedCode, ctx.runNo() + ".csv"),
                    new DropContent(CSV, csv(sent).getBytes(StandardCharsets.UTF_8)),
                    new ExtractFile.Origin("OPSLEDGER", ctx.runNo()));
              }
            });
    return run.getRunNo();
  }

  static String csv(List<FeedItem> items) {
    Set<String> headers = new LinkedHashSet<>();
    headers.add("key");
    items.forEach(i -> headers.addAll(i.fields().keySet()));
    StringBuilder out = new StringBuilder(String.join(",", headers)).append('\n');
    for (FeedItem item : items) {
      List<String> cells = new ArrayList<>();
      for (String h : headers) {
        cells.add(quote("key".equals(h) ? item.key() : item.fields().getOrDefault(h, "")));
      }
      out.append(String.join(",", cells)).append('\n');
    }
    return out.toString();
  }

  private static String quote(String value) {
    String v = value == null ? "" : value;
    return v.contains(",") || v.contains("\"") || v.contains("\n")
        ? "\"" + v.replace("\"", "\"\"") + "\""
        : v;
  }
}
