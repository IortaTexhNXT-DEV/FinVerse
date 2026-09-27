package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.booking.domain.CommissionTerms;
import com.iortatechnxt.brokerverse.booking.domain.InsurerShare;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponents;
import com.iortatechnxt.brokerverse.booking.service.port.LegacyInvoiceSource;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceData.Classification;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The legacy originals of the migrated accounts for the booking endorsements (DATA_MIGRATION_DESIGN
 * 14.4 H): the legacy invoice of the policy year (not cancelled, not an endorsement) with its
 * premium by component as booked and adjusted, its commission, rate, flags and insurer shares.
 */
@Component
@Transactional(readOnly = true)
public class OpsLegacyInvoiceSource implements LegacyInvoiceSource {

  private static final int RATE_SCALE = 8;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final OpsInvoiceRepository invoices;

  /**
   * Creates the source.
   *
   * @param invoices operations invoices
   */
  public OpsLegacyInvoiceSource(OpsInvoiceRepository invoices) {
    this.invoices = invoices;
  }

  @Override
  public Optional<LegacyOriginal> original(Long companyId, String arn, LocalDate effectiveDate) {
    return invoices.findByArnOrderByPolicyYearAscIdAsc(arn).stream()
        .filter(i -> i.getCompanyId().equals(companyId))
        .filter(i -> i.getLegacy().isLegacy() && i.getRecordOrigin().isMigrated())
        .filter(i -> i.getKind() == InvoiceKind.BOOKING && !i.isCancelled())
        .filter(i -> covers(i.getClassification(), effectiveDate))
        .findFirst()
        .map(OpsLegacyInvoiceSource::originalOf);
  }

  private static boolean covers(Classification c, LocalDate date) {
    return !date.isBefore(c.inceptionDate()) && date.isBefore(c.expiryDate());
  }

  private static LegacyOriginal originalOf(OpsInvoice i) {
    Map<LedgerComponent, BigDecimal> amount = new EnumMap<>(LedgerComponent.class);
    for (OpsInvoiceComponent c : i.getComponents()) {
      amount.put(c.getComponent(), c.getBooked().add(c.getAdjusted()));
    }
    PremiumComponents premium =
        new PremiumComponents(
            of(amount, LedgerComponent.BASIC),
            of(amount, LedgerComponent.DST),
            of(amount, LedgerComponent.PREMIUM_TAX_VAT),
            of(amount, LedgerComponent.LGT),
            of(amount, LedgerComponent.FST),
            of(amount, LedgerComponent.OTHER));
    BigDecimal basic = premium.basic();
    BigDecimal rate =
        basic.signum() == 0
            ? BigDecimal.ZERO
            : i.getCommission().multiply(HUNDRED).divide(basic, RATE_SCALE, RoundingMode.HALF_UP);
    Classification c = i.getClassification();
    return new LegacyOriginal(
        i.getInvoiceNo(),
        i.getBranchId(),
        i.getPolicyYear(),
        i.getPolicyNo(),
        i.getCurrency(),
        c.costCenter(),
        c.bookingDate(),
        c.inceptionDate(),
        c.expiryDate(),
        premium,
        CommissionTerms.of(rate, i.getCommission(), i.getVatOnCommission(), i.getWtaxRate()),
        i.isDpFlag(),
        i.isCwtFlag(),
        i.getShares().stream().map(s -> new InsurerShare(s.insurerCode(), s.sharePct())).toList());
  }

  private static BigDecimal of(Map<LedgerComponent, BigDecimal> amounts, LedgerComponent c) {
    return amounts.getOrDefault(c, BigDecimal.ZERO);
  }
}
