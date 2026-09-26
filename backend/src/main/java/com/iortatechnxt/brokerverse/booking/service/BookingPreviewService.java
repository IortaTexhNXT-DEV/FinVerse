package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.accounting.service.AccountingRuleService;
import com.iortatechnxt.brokerverse.accounting.service.AccountingRuleService.Simulation;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceDraft;
import com.iortatechnxt.brokerverse.booking.service.BookingEvents.ShareEvent;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pre-booking confirmation (BRNB.036): the invoice(s) an account would be booked with and the
 * journal the accounting rules would generate, without numbering or posting anything. Also used for
 * the live journal preview of an endorsement.
 */
@Service
@Transactional(readOnly = true)
public class BookingPreviewService {

  /** Reference shown in place of the invoice number before booking. */
  public static final String PREVIEW = "PREVIEW";

  private final BookingService booking;
  private final InvoiceBuilder builder;
  private final BookingEvents events;
  private final AccountingRuleService rules;
  private final ChartOfAccountsService chart;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param booking bookability check
   * @param builder invoice builder
   * @param events event builder
   * @param rules accounting rule preview
   * @param chart account names
   * @param clock clock
   */
  public BookingPreviewService(
      BookingService booking,
      InvoiceBuilder builder,
      BookingEvents events,
      AccountingRuleService rules,
      ChartOfAccountsService chart,
      Clock clock) {
    this.booking = booking;
    this.builder = builder;
    this.events = events;
    this.rules = rules;
    this.chart = chart;
    this.clock = clock;
  }

  /**
   * Previews the booking of an account.
   *
   * @param arn account
   * @param options booking choices
   * @return invoices (first year booked now, later years scheduled) and journal
   */
  public BookingPreview preview(String arn, BookingOptions options) {
    Account account = booking.requireBookable(arn);
    LocalDate date = options.bookingDate() == null ? LocalDate.now(clock) : options.bookingDate();
    List<InvoiceDraft> drafts = builder.drafts(account, options, date);
    return new BookingPreview(date, drafts, journal(drafts.get(0), date));
  }

  /**
   * The journal lines an invoice draft would post.
   *
   * @param draft invoice draft
   * @param date booking date
   * @return lines of every insurer share
   */
  public List<PreviewLine> journal(InvoiceDraft draft, LocalDate date) {
    List<PreviewLine> lines = new ArrayList<>();
    for (ShareEvent share : events.events(BookingEvents.factsOf(draft, PREVIEW, date))) {
      if (!share.hasAmounts()) {
        continue;
      }
      Simulation simulation = rules.preview(share.event());
      for (JournalLineRequest line : simulation.lines()) {
        lines.add(
            new PreviewLine(
                share.insurerCode(),
                line.accountCode(),
                accountName(draft.companyId(), line.accountCode()),
                line.side(),
                line.amount(),
                line.partyCode(),
                line.narration()));
      }
    }
    return lines;
  }

  private String accountName(Long companyId, String code) {
    try {
      return chart.getByCode(companyId, code).getName();
    } catch (ResourceNotFoundException ex) {
      return code;
    }
  }

  /**
   * Pre-booking confirmation.
   *
   * @param bookingDate booking date
   * @param invoices invoices, the first booked now and the others scheduled (multi-year)
   * @param journal journal lines of the first invoice
   */
  public record BookingPreview(
      LocalDate bookingDate, List<InvoiceDraft> invoices, List<PreviewLine> journal) {

    /** Defensive copies. */
    public BookingPreview {
      invoices = List.copyOf(invoices);
      journal = List.copyOf(journal);
    }
  }

  /**
   * One journal line of a preview.
   *
   * @param insurerCode insurer share the line belongs to
   * @param accountCode GL account
   * @param accountName account name
   * @param side debit or credit
   * @param amount amount
   * @param partyCode sub-ledger party
   * @param narration narration
   */
  public record PreviewLine(
      String insurerCode,
      String accountCode,
      String accountName,
      BalanceSide side,
      BigDecimal amount,
      String partyCode,
      String narration) {}
}
