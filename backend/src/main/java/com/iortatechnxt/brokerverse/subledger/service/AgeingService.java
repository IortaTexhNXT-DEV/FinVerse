package com.iortatechnxt.brokerverse.subledger.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.subledger.domain.ItemDirection;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ages outstanding open items into configurable day buckets (debtors / creditors aged analysis).
 *
 * <p>Amounts are signed from the company's point of view: DEBIT items positive (owed to the
 * company), CREDIT items negative. Base currency amounts are pro-rated by the outstanding share.
 */
@Service
@Transactional(readOnly = true)
public class AgeingService {

  /** Default buckets: current, 1-30, 31-60, 61-90, 91-180, 181-365, over 365 days. */
  public static final List<AgeingBucket> DEFAULT_BUCKETS =
      List.of(
          new AgeingBucket("Not due", Integer.MIN_VALUE, 0),
          new AgeingBucket("1-30", 1, 30),
          new AgeingBucket("31-60", 31, 60),
          new AgeingBucket("61-90", 61, 90),
          new AgeingBucket("91-180", 91, 180),
          new AgeingBucket("181-365", 181, 365),
          new AgeingBucket("Over 365", 366, null));

  private final OpenItemService openItems;
  private final SystemParameterService parameters;

  /**
   * Creates the service.
   *
   * @param openItems open item service
   * @param parameters system parameters ({@code AGEING_BUCKETS})
   */
  public AgeingService(OpenItemService openItems, SystemParameterService parameters) {
    this.openItems = openItems;
    this.parameters = parameters;
  }

  /**
   * Company-wide default ageing slots of the debtors and creditors reports: the {@code
   * AGEING_BUCKETS} system parameter, or {@link AgeingSlots#STANDARD} when it is blank or not a
   * valid slot list (reports must keep working while an administrator fixes the parameter).
   *
   * @return default slots
   */
  public AgeingSlots defaultSlots() {
    String configured = String.join(",", parameters.items(SystemParameterService.AGEING_BUCKETS));
    try {
      return AgeingSlots.parse(configured, AgeingSlots.STANDARD);
    } catch (BusinessRuleException invalid) {
      return AgeingSlots.STANDARD;
    }
  }

  /**
   * Slots of a report run: the slots the user entered, else the default.
   *
   * @param text slot text such as "30,60,90,120", blank for the default
   * @return slots
   */
  public AgeingSlots slotsOrDefault(String text) {
    return text == null || text.isBlank() ? defaultSlots() : AgeingSlots.parse(text, null);
  }

  /**
   * Ages outstanding items per party.
   *
   * @param companyId company
   * @param asOf ageing date
   * @param buckets buckets (use {@link #DEFAULT_BUCKETS} when in doubt)
   * @param filter item filter (e.g. by party type, branch or document type)
   * @return one row per party, ordered by party code
   */
  public List<AgeingRow> age(
      Long companyId, LocalDate asOf, List<AgeingBucket> buckets, Predicate<OpenItem> filter) {
    Map<String, BigDecimal[]> byParty = new LinkedHashMap<>();
    for (OpenItem item : openItems.outstanding(companyId, asOf)) {
      if (!filter.test(item)) {
        continue;
      }
      long days = ChronoUnit.DAYS.between(item.getDueDate(), asOf);
      BigDecimal[] amounts =
          byParty.computeIfAbsent(item.getPartyCode(), k -> zeros(buckets.size()));
      for (int i = 0; i < buckets.size(); i++) {
        if (buckets.get(i).contains(days)) {
          amounts[i] = amounts[i].add(signedBaseOutstanding(item));
        }
      }
    }
    List<AgeingRow> rows = new ArrayList<>();
    byParty.forEach((party, amounts) -> rows.add(new AgeingRow(party, List.of(amounts))));
    return rows;
  }

  /**
   * Outstanding base-currency amount, signed (DEBIT positive).
   *
   * @param item item
   * @return signed amount
   */
  public static BigDecimal signedBaseOutstanding(OpenItem item) {
    BigDecimal base =
        item.getAmount().signum() == 0
            ? BigDecimal.ZERO
            : item.getBaseAmount()
                .multiply(item.outstanding())
                .divide(item.getAmount(), Money.SCALE, Money.ROUNDING);
    return item.getDirection() == ItemDirection.DEBIT ? base : base.negate();
  }

  private static BigDecimal[] zeros(int n) {
    BigDecimal[] a = new BigDecimal[n];
    Arrays.fill(a, BigDecimal.ZERO);
    return a;
  }

  /**
   * Ageing result for one party.
   *
   * @param partyCode party
   * @param buckets amounts per bucket, in bucket order
   */
  public record AgeingRow(String partyCode, List<BigDecimal> buckets) {

    /**
     * Total across buckets.
     *
     * @return total
     */
    public BigDecimal total() {
      return buckets.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
  }
}
