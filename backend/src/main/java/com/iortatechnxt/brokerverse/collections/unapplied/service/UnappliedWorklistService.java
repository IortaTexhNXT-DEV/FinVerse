package com.iortatechnxt.brokerverse.collections.unapplied.service;

import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.service.CollectionItems;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.UnappliedDisposition;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.UnappliedDispositionRepository;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedFilter;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedView;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The collector view of the unapplied payments (BRCLXN.034-036): Cashiering's open items as of
 * today, read through {@link UnappliedDirectory}, with their age, the account fields of the matched
 * invoice ({@link AccountFacts}) and the latest collector disposition. The Cashiering filters
 * (text, client, sales unit, tab, payment dates, age) run in Cashiering; the collector filters
 * (market segment, collector disposition) run here over at most {@value #FILTER_CAP} items.
 */
@Service
@Transactional(readOnly = true)
public class UnappliedWorklistService {

  /** Collector disposition filter value of the items without one. */
  public static final String NO_DISPOSITION = "NONE";

  /** Most items read for a collector filter. */
  static final int FILTER_CAP = 2000;

  private final UnappliedDirectory directory;
  private final UnappliedDispositionRepository dispositions;
  private final CollectionItems items;
  private final InvoiceLedgerQueryService ledger;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param directory Cashiering's unapplied items
   * @param dispositions collector dispositions
   * @param items collection items
   * @param ledger invoice ledger
   * @param clock clock
   */
  public UnappliedWorklistService(
      UnappliedDirectory directory,
      UnappliedDispositionRepository dispositions,
      CollectionItems items,
      InvoiceLedgerQueryService ledger,
      Clock clock) {
    this.directory = directory;
    this.dispositions = dispositions;
    this.items = items;
    this.ledger = ledger;
    this.clock = clock;
  }

  /**
   * The open unapplied items of a company.
   *
   * @param companyId company
   * @param filter criteria
   * @param pageable page
   * @return rows, latest payment first
   */
  public Page<CollectorRow> list(Long companyId, CollectorFilter filter, Pageable pageable) {
    LocalDate today = LocalDate.now(clock);
    UnappliedFilter base =
        new UnappliedFilter(
            filter.text(),
            filter.clientCode(),
            filter.salesUnit(),
            filter.tab(),
            filter.ageMax() == null ? filter.paidFrom() : today.minusDays(filter.ageMax()),
            filter.ageMin() == null ? filter.paidTo() : today.minusDays(filter.ageMin()));
    if (!filter.collectorSide()) {
      Page<UnappliedView> page = directory.open(companyId, base, pageable);
      return new PageImpl<>(rows(companyId, page.getContent()), pageable, page.getTotalElements());
    }
    List<CollectorRow> all =
        rows(companyId, directory.open(companyId, base, PageRequest.of(0, FILTER_CAP)).getContent())
            .stream()
            .filter(filter::accepts)
            .toList();
    int from = (int) Math.min(pageable.getOffset(), all.size());
    int to = Math.min(from + pageable.getPageSize(), all.size());
    return new PageImpl<>(all.subList(from, to), pageable, all.size());
  }

  /**
   * One item as the collector sees it.
   *
   * @param companyId company
   * @param view item
   * @return row
   */
  public CollectorRow row(Long companyId, UnappliedView view) {
    return rows(companyId, List.of(view)).get(0);
  }

  private List<CollectorRow> rows(Long companyId, List<UnappliedView> views) {
    if (views.isEmpty()) {
      return List.of();
    }
    LocalDate today = LocalDate.now(clock);
    Map<String, UnappliedDisposition> latest =
        dispositions
            .latest(companyId, views.stream().map(UnappliedView::unappliedRef).toList())
            .stream()
            .collect(Collectors.toMap(UnappliedDisposition::getUnappliedRef, Function.identity()));
    Map<String, AccountFacts> facts = facts(companyId, views);
    return views.stream()
        .map(
            v ->
                new CollectorRow(
                    v,
                    (int) ChronoUnit.DAYS.between(v.paymentDate(), today),
                    v.matchedInvoiceNo() == null ? null : facts.get(v.matchedInvoiceNo()),
                    latest.get(v.unappliedRef())))
        .toList();
  }

  private Map<String, AccountFacts> facts(Long companyId, List<UnappliedView> views) {
    List<String> invoiceNos =
        views.stream()
            .map(UnappliedView::matchedInvoiceNo)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    Map<String, AccountFacts> facts = new HashMap<>();
    if (invoiceNos.isEmpty()) {
      return facts;
    }
    for (CollectionItem i : items.of(companyId, invoiceNos)) {
      facts.put(i.getInvoiceNo(), AccountFacts.of(i));
    }
    for (String no : invoiceNos) {
      if (!facts.containsKey(no)) {
        ledger
            .find(no)
            .filter(i -> i.getCompanyId().equals(companyId))
            .ifPresent(i -> facts.put(no, AccountFacts.of(i)));
      }
    }
    return facts;
  }

  /**
   * An unapplied item with the collector's fields.
   *
   * @param item Cashiering's item
   * @param ageDays days since the payment date
   * @param account account of the matched invoice, may be null
   * @param disposition latest collector disposition, may be null
   */
  public record CollectorRow(
      UnappliedView item, int ageDays, AccountFacts account, UnappliedDisposition disposition) {}

  /**
   * Criteria of the collector list (BRCLXN.035). Null fields do not filter.
   *
   * @param text reference, payor, transaction, check, reference or invoice
   * @param clientCode matched client
   * @param salesUnit marketing unit
   * @param tab Cashiering tab
   * @param paidFrom payment date from
   * @param paidTo payment date to
   * @param ageMin minimum age in days
   * @param ageMax maximum age in days
   * @param segment market segment of the matched invoice
   * @param disposition latest collector disposition, or {@value #NO_DISPOSITION}
   */
  public record CollectorFilter(
      String text,
      String clientCode,
      String salesUnit,
      String tab,
      LocalDate paidFrom,
      LocalDate paidTo,
      Integer ageMin,
      Integer ageMax,
      String segment,
      String disposition) {

    /**
     * Whether a collector-side filter is set.
     *
     * @return true when segment or disposition filters
     */
    boolean collectorSide() {
      return segment != null || disposition != null;
    }

    /**
     * Whether a row passes the collector-side filters.
     *
     * @param row row
     * @return true when kept
     */
    boolean accepts(CollectorRow row) {
      boolean segmentOk =
          segment == null || row.account() != null && segment.equals(row.account().segment());
      String code =
          row.disposition() == null ? NO_DISPOSITION : row.disposition().getDispositionCode();
      return segmentOk && (disposition == null || disposition.equals(code));
    }
  }
}
