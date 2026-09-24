package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceRelatedItems;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLineRepository;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequest;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequestRepository;
import com.iortatechnxt.brokerverse.remittance.domain.InvoiceTag;
import com.iortatechnxt.brokerverse.remittance.domain.InvoiceTagRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.SpecialRemittance;
import com.iortatechnxt.brokerverse.remittance.domain.SpecialRemittanceRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Remittances tab of Invoice 360 (RMTID.026/032/036/038): the batches the invoice was in with
 * their stage, amounts, exclusion and insurer OR, its hold requests, special remittance requests
 * and its current extraction tag.
 */
@Component
@Transactional(readOnly = true)
public class RemittanceRelatedItems implements InvoiceRelatedItems {

  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private final BatchLineRepository lines;
  private final HoldRequestRepository holds;
  private final SpecialRemittanceRepository specials;
  private final InvoiceTagRepository tags;

  /**
   * Creates the source.
   *
   * @param lines batch lines
   * @param holds hold requests
   * @param specials special remittance requests
   * @param tags extraction tags
   */
  public RemittanceRelatedItems(
      BatchLineRepository lines,
      HoldRequestRepository holds,
      SpecialRemittanceRepository specials,
      InvoiceTagRepository tags) {
    this.lines = lines;
    this.holds = holds;
    this.specials = specials;
    this.tags = tags;
  }

  @Override
  public Section section() {
    return Section.REMITTANCES;
  }

  @Override
  public List<RelatedItem> itemsFor(String invoiceNo) {
    List<RelatedItem> items = new ArrayList<>();
    lines.findByInvoiceNo(invoiceNo).forEach(l -> items.add(batchItem(l)));
    holds.findByInvoiceInvoiceNoOrderByIdDesc(invoiceNo).forEach(h -> items.add(holdItem(h)));
    specials.findByInvoiceInvoiceNoOrderByIdDesc(invoiceNo).forEach(s -> items.add(specialItem(s)));
    tags.findFirstByInvoiceNoOrderByIdDesc(invoiceNo).ifPresent(t -> items.add(tagItem(t)));
    return items;
  }

  private static RelatedItem batchItem(BatchLine l) {
    RemittanceBatch b = l.getBatch();
    StringBuilder text = new StringBuilder(BatchDocuments.typeLabel(b.getRemittanceType()));
    if (l.isExcluded()) {
      text.append(" - excluded: ").append(l.getExclusionReason());
    }
    if (l.getInsurerOrNo() != null) {
      text.append(" - insurer OR ").append(l.getInsurerOrNo());
    }
    if (b.getDvNo() != null) {
      text.append(" - DV ").append(b.getDvNo());
    }
    return new RelatedItem(
        "REMITTANCE_BATCH",
        b.getBatchNo(),
        date(b.getCreatedAt()),
        l.getAmounts().paidAr(),
        l.isExcluded() ? "EXCLUDED" : b.getStage().name(),
        text.toString(),
        "/remittance/batches/" + b.getId());
  }

  private static RelatedItem holdItem(HoldRequest h) {
    return new RelatedItem(
        "HOLD",
        h.getRequestNo(),
        date(h.getCreatedAt()),
        null,
        h.getStage().name(),
        "Hold until " + h.getHoldUntil() + " (" + h.getReasonCode() + ")",
        "/remittance/holds/" + h.getId());
  }

  private static RelatedItem specialItem(SpecialRemittance s) {
    return new RelatedItem(
        "SPECIAL_REMITTANCE",
        s.getRequestNo(),
        date(s.getCreatedAt()),
        null,
        s.getStage().name(),
        "Special remittance - " + s.getConditionCode(),
        "/remittance/special/" + s.getId());
  }

  private static RelatedItem tagItem(InvoiceTag t) {
    return new RelatedItem(
        "EXTRACTION_TAG",
        t.getBatchNo() == null ? t.getTag().name() : t.getBatchNo(),
        date(t.getCreatedAt()),
        t.getRemittable(),
        t.getTag().name(),
        t.getReasons() == null ? "Current extraction tag" : "Reasons: " + t.getReasons(),
        "/remittance/extraction");
  }

  private static LocalDate date(Instant at) {
    return at == null ? null : LocalDate.ofInstant(at, MANILA);
  }
}
