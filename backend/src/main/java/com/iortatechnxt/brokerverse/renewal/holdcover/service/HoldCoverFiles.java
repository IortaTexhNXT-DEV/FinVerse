package com.iortatechnxt.brokerverse.renewal.holdcover.service;

import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.renewal.domain.HoldCoverAsk;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The hold cover request files (FRRN.036.04, FRRN.036.06): a workbook of the requests of an
 * insurer, one row per renewal account with its reference number, sent to the insurer by MFT or
 * e-mail and kept on the renewal accounts.
 */
@Component
public class HoldCoverFiles {

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final List<String> HEADERS =
      List.of(
          "Reference Number",
          "Renewal Reference Number",
          "Assured's Name",
          "Expiring Policy No.",
          "Expiry Date",
          "Hold Cover Start",
          "Hold Cover End",
          "Days",
          "Share (%)",
          "Request");

  private final DocumentComposer composer;
  private final InsurerDelivery delivery;

  /**
   * Creates the files.
   *
   * @param composer workbook
   * @param delivery MFT or e-mail to the insurer
   */
  public HoldCoverFiles(DocumentComposer composer, InsurerDelivery delivery) {
    this.composer = composer;
    this.delivery = delivery;
  }

  /**
   * Sends the file of one request.
   *
   * @param c renewal
   * @param ask request
   */
  public void sendOne(RenewalCandidate c, HoldCoverAsk ask) {
    send(ask.getInsurerCode(), List.of(new Line(c, ask)), "HOLD_COVER_" + ask.getRequestNo(), null);
  }

  /**
   * Sends the file of the requests of an insurer.
   *
   * @param insurerCode insurer
   * @param lines renewal accounts and their requests
   * @param name file name without extension
   * @param batchNo batch number recorded on the requests
   */
  public void send(String insurerCode, List<Line> lines, String name, String batchNo) {
    List<List<Object>> rows = new ArrayList<>();
    for (Line l : lines) {
      RenewalCandidate c = l.candidate();
      HoldCoverAsk a = l.ask();
      List<Object> row = new ArrayList<>();
      row.add(a.getRequestNo());
      row.add(c.getRenewalRef());
      row.add(c.getSnapshot().clientName());
      row.add(c.getSnapshot().policyNo());
      row.add(c.getExpiryDate());
      row.add(a.getStartDate());
      row.add(a.getEndDate());
      row.add(a.getDays());
      row.add(a.getSharePercent());
      row.add(a.getKind());
      rows.add(row);
    }
    MessageFile file =
        new MessageFile(
            name + ".xlsx",
            XLSX,
            composer.xlsx(new SheetSpec("Hold cover requests", HEADERS, rows)));
    InsurerDelivery.Sent sent =
        delivery.send(
            lines.get(0).candidate().getCompanyId(),
            insurerCode,
            file,
            lines.stream().map(Line::candidate).toList(),
            "HOLD_COVER");
    lines.forEach(l -> l.ask().sent(sent.channel(), sent.message(), batchNo));
  }

  /**
   * A renewal account in a request file.
   *
   * @param candidate renewal
   * @param ask its request to the insurer
   */
  public record Line(RenewalCandidate candidate, HoldCoverAsk ask) {}
}
