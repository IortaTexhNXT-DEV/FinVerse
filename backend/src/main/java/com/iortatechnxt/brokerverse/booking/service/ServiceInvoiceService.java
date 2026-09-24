package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoiceRepository;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoiceType;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoiceTypeRepository;
import com.iortatechnxt.brokerverse.booking.domain.SiKind;
import com.iortatechnxt.brokerverse.booking.domain.SiRecipient;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service invoices and credits (BRNB.100/100b; ADJID.014) - mandatory contract for Operations.
 *
 * <ul>
 *   <li>{@link #issue} issues a service invoice of a type: number {@code SI-<branch>-<yyyy>-n} from
 *       the branch's gap-free series (allocated in the caller's transaction, so a rollback frees
 *       nothing and leaves no gap), PDF from the type's template, e-mail to the insurer's billing
 *       address and owner notification.
 *   <li>{@link #credit} credits all or part of a service invoice.
 *   <li>Booking issues the types triggered {@code ON_BOOKING} (and positive endorsements those
 *       {@code ON_ENDORSEMENT}), one per insurer share; return invoices credit them ({@link
 *       ServiceInvoiceTriggers}).
 * </ul>
 */
@Service
@Transactional
public class ServiceInvoiceService {

  private static final int SCALE = 2;

  private final ServiceInvoiceRepository serviceInvoices;
  private final ServiceInvoiceTypeRepository types;
  private final DocumentNumberService numbers;
  private final ServiceInvoiceDocument document;
  private final ServiceInvoiceDispatch dispatch;
  private final PartyService parties;
  private final BookingSettings settings;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param serviceInvoices service invoices
   * @param types service invoice types
   * @param numbers document numbers
   * @param document PDF renderer
   * @param dispatch e-mail dispatch
   * @param parties recipients
   * @param settings booking branch
   * @param audit audit trail
   * @param clock clock
   */
  public ServiceInvoiceService(
      ServiceInvoiceRepository serviceInvoices,
      ServiceInvoiceTypeRepository types,
      DocumentNumberService numbers,
      ServiceInvoiceDocument document,
      ServiceInvoiceDispatch dispatch,
      PartyService parties,
      BookingSettings settings,
      AuditTrailService audit,
      Clock clock) {
    this.serviceInvoices = serviceInvoices;
    this.types = types;
    this.numbers = numbers;
    this.document = document;
    this.dispatch = dispatch;
    this.parties = parties;
    this.settings = settings;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Issues a service invoice (contract).
   *
   * @param request type, recipient, references and amounts
   * @return the service invoice, numbered, rendered and queued for e-mail
   */
  public ServiceInvoice issue(IssueRequest request) {
    ServiceInvoiceType type = requireActiveType(request.typeCode());
    Recipient recipient = recipient(request.companyId(), type, request);
    ServiceInvoice.Values values =
        new ServiceInvoice.Values(
            type.getCode(),
            SiKind.INVOICE,
            request.invoiceNo(),
            request.arn(),
            recipient.code(),
            recipient.name(),
            recipient.email(),
            request.issueDate() == null ? LocalDate.now(clock) : request.issueDate(),
            request.currency(),
            money(request.commission()),
            money(request.vatOnCommission()),
            money(request.wtaxAmount()),
            type.getOwnerUsername(),
            type.getOwnerPermission(),
            null,
            request.remarks());
    return save(request.companyId(), values, type.getTemplateCode(), "Issued");
  }

  /**
   * Credits a service invoice (contract): the remaining commission and VAT, or the amounts given.
   *
   * @param siNo service invoice to credit
   * @param request amounts (null for the remainder) and reason
   * @return the credit
   */
  public ServiceInvoice credit(String siNo, CreditRequest request) {
    ServiceInvoice original = requireBySiNo(siNo);
    if (original.getKind() != SiKind.INVOICE) {
      throw new BusinessRuleException("CREDIT_OF_CREDIT", siNo + " is itself a credit");
    }
    Remaining left = remaining(original);
    BigDecimal commission = orElse(request.commission(), left.commission());
    BigDecimal vat = orElse(request.vatOnCommission(), left.vat());
    if (!left.allows(commission, vat)) {
      throw new BusinessRuleException(
          "CREDIT_EXCEEDS_INVOICE",
          "Service invoice "
              + siNo
              + " has "
              + left.commission()
              + " commission and "
              + left.vat()
              + " VAT left to credit");
    }
    ServiceInvoice.Values values =
        new ServiceInvoice.Values(
            original.getTypeCode(),
            SiKind.CREDIT,
            request.invoiceNo() == null ? original.getInvoiceNo() : request.invoiceNo(),
            original.getArn(),
            original.getRecipientCode(),
            original.getRecipientName(),
            original.getRecipientEmail(),
            LocalDate.now(clock),
            original.getCurrency(),
            commission,
            vat,
            proportion(original.getWtaxAmount(), commission, original.getCommission()),
            original.getOwnerUsername(),
            original.getOwnerPermission(),
            original.getSiNo(),
            request.reason());
    String template = requireType(original.getTypeCode()).getTemplateCode();
    return save(original.getCompanyId(), values, template, "Credit of " + siNo + ":");
  }

  /**
   * Sends a service invoice again (BRNB.100b register).
   *
   * @param id service invoice
   * @return service invoice, queued again
   */
  public ServiceInvoice resend(Long id) {
    ServiceInvoice si = get(id);
    String email = si.getRecipientEmail();
    if (email == null && requireType(si.getTypeCode()).getRecipient() == SiRecipient.INSURER) {
      email = parties.getByCode(si.getCompanyId(), si.getRecipientCode()).getEmail();
    }
    dispatch.send(si, email);
    audit.record(ServiceInvoiceDispatch.ENTITY, si.getSiNo(), AuditAction.UPDATE, "Resent");
    return si;
  }

  /**
   * A service invoice.
   *
   * @param id id
   * @return service invoice
   */
  @Transactional(readOnly = true)
  public ServiceInvoice get(Long id) {
    return serviceInvoices
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ServiceInvoiceDispatch.ENTITY, id));
  }

  /**
   * A service invoice by number.
   *
   * @param siNo number
   * @return service invoice
   */
  @Transactional(readOnly = true)
  public ServiceInvoice requireBySiNo(String siNo) {
    return serviceInvoices
        .findBySiNo(siNo)
        .orElseThrow(() -> new ResourceNotFoundException(ServiceInvoiceDispatch.ENTITY, siNo));
  }

  /**
   * What is left to credit on a service invoice.
   *
   * @param original service invoice
   * @return commission and VAT not yet credited
   */
  @Transactional(readOnly = true)
  public Remaining remaining(ServiceInvoice original) {
    BigDecimal commission = original.getCommission();
    BigDecimal vat = original.getVatOnCommission();
    for (ServiceInvoice credit : serviceInvoices.findByCreditOfOrderByIdAsc(original.getSiNo())) {
      commission = commission.subtract(credit.getCommission());
      vat = vat.subtract(credit.getVatOnCommission());
    }
    return new Remaining(commission, vat);
  }

  private ServiceInvoice save(
      Long companyId, ServiceInvoice.Values values, String template, String action) {
    BookingSettings.BranchRef branch = settings.branch(companyId);
    String number = numbers.next("SI-" + branch.code() + "-" + values.issueDate().getYear());
    ServiceInvoice si = new ServiceInvoice(companyId, branch.id(), number, values);
    document.render(si, template);
    serviceInvoices.save(si);
    dispatch.send(si, values.recipientEmail());
    audit.record(
        ServiceInvoiceDispatch.ENTITY,
        number,
        AuditAction.CREATE,
        action
            + " "
            + values.kind()
            + " "
            + number
            + " to "
            + values.recipientCode()
            + ", net "
            + si.getNetAmount().toPlainString());
    return si;
  }

  private Recipient recipient(Long companyId, ServiceInvoiceType type, IssueRequest request) {
    if (type.getRecipient() == SiRecipient.INTERNAL) {
      String name =
          request.recipientName() == null ? request.recipientCode() : request.recipientName();
      return new Recipient(request.recipientCode(), name, null);
    }
    Party insurer =
        parties.requireActive(companyId, request.recipientCode(), List.of(PartyType.INSURER));
    return new Recipient(insurer.getCode(), insurer.getName(), insurer.getEmail());
  }

  private ServiceInvoiceType requireActiveType(String code) {
    ServiceInvoiceType type = requireType(code);
    if (!type.isActive()) {
      throw new BusinessRuleException(
          "SERVICE_INVOICE_TYPE_INACTIVE", "Service invoice type " + code + " is inactive");
    }
    return type;
  }

  private ServiceInvoiceType requireType(String code) {
    return types
        .findByCode(code)
        .orElseThrow(() -> new ResourceNotFoundException("Service invoice type", code));
  }

  private static BigDecimal proportion(BigDecimal amount, BigDecimal part, BigDecimal whole) {
    if (whole.signum() == 0) {
      return BigDecimal.ZERO.setScale(SCALE);
    }
    return amount.multiply(part).divide(whole, SCALE, RoundingMode.HALF_UP);
  }

  private static BigDecimal money(BigDecimal amount) {
    return amount == null
        ? BigDecimal.ZERO.setScale(SCALE)
        : amount.setScale(SCALE, RoundingMode.HALF_UP);
  }

  /**
   * A service invoice to issue.
   *
   * @param companyId company
   * @param typeCode service invoice type
   * @param invoiceNo booked invoice, null for a manual one
   * @param arn account, may be null
   * @param recipientCode insurer party code, or the internal unit
   * @param recipientName name of an internal recipient (insurers are named from their party)
   * @param issueDate issue date, null for today
   * @param currency currency
   * @param commission commission line
   * @param vatOnCommission VAT line
   * @param wtaxAmount withholding tax line
   * @param remarks remarks
   */
  public record IssueRequest(
      Long companyId,
      String typeCode,
      String invoiceNo,
      String arn,
      String recipientCode,
      String recipientName,
      LocalDate issueDate,
      String currency,
      BigDecimal commission,
      BigDecimal vatOnCommission,
      BigDecimal wtaxAmount,
      String remarks) {}

  /**
   * A credit to issue.
   *
   * @param commission commission to credit, null for all that is left
   * @param vatOnCommission VAT to credit, null for all that is left
   * @param invoiceNo return invoice that caused it, null to keep the original's
   * @param reason reason
   */
  public record CreditRequest(
      BigDecimal commission, BigDecimal vatOnCommission, String invoiceNo, String reason) {}

  private record Recipient(String code, String name, String email) {}

  private static BigDecimal orElse(BigDecimal value, BigDecimal fallback) {
    return value == null ? fallback : money(value);
  }

  /**
   * Commission and VAT left to credit on a service invoice.
   *
   * @param commission commission left
   * @param vat VAT left
   */
  public record Remaining(BigDecimal commission, BigDecimal vat) {

    /** Whether a credit of these amounts is possible: not negative, not empty, not too much. */
    public boolean allows(BigDecimal c, BigDecimal v) {
      boolean negative = c.signum() < 0 || v.signum() < 0;
      boolean tooMuch = c.compareTo(commission) > 0 || v.compareTo(vat) > 0;
      boolean nothing = c.signum() == 0 && v.signum() == 0;
      return !negative && !tooMuch && !nothing;
    }
  }
}
