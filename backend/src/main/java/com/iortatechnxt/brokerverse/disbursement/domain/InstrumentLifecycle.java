package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The instrument status machine of each mode of payment (DIS 2.8.1-2.8.4, 3.26.1-3.26.7; design
 * 7.2):
 *
 * <pre>
 * CHECK          PENDING -print-> PRINTED -release-> RELEASED -negotiated(file)-> NEGOTIATED;
 *                PRINTED / RELEASED -DISB_STALE_DAYS-> STALE
 * ATD            PENDING -pdf-> PRINTED -email-> EMAILED -branch confirmed-> DEBITED
 * CTA            PENDING -DCTF-> EXTRACTED -credited(file)-> CREDITED
 * CREDIT_TICKET  PENDING -form-> PRINTED -branch confirmed-> DEBITED   (TT the same)
 * MC_DD          PENDING -form-> PRINTED -received from branch-> RECEIVED -release-> RELEASED
 * ONLINE_BANKING APPROVED -BOB approved(voucher ref)-> DEBITED
 * any status that is not final -cancel-> CANCELLED
 * </pre>
 *
 * The payment counts as made (gateway status PAID) once the instrument reaches a paid status.
 */
public final class InstrumentLifecycle {

  private static final Map<DisbursementMode, Map<InstrumentStatus, Set<InstrumentStatus>>> NEXT =
      new EnumMap<>(DisbursementMode.class);

  private static final Set<InstrumentStatus> FINAL =
      EnumSet.of(
          InstrumentStatus.NEGOTIATED,
          InstrumentStatus.STALE,
          InstrumentStatus.CANCELLED,
          InstrumentStatus.CREDITED,
          InstrumentStatus.DEBITED);

  static {
    NEXT.put(
        DisbursementMode.CHECK,
        Map.of(
            InstrumentStatus.PENDING, EnumSet.of(InstrumentStatus.PRINTED),
            InstrumentStatus.PRINTED, EnumSet.of(InstrumentStatus.RELEASED, InstrumentStatus.STALE),
            InstrumentStatus.RELEASED,
                EnumSet.of(InstrumentStatus.NEGOTIATED, InstrumentStatus.STALE)));
    NEXT.put(
        DisbursementMode.ATD,
        Map.of(
            InstrumentStatus.PENDING, EnumSet.of(InstrumentStatus.PRINTED),
            InstrumentStatus.PRINTED, EnumSet.of(InstrumentStatus.EMAILED),
            InstrumentStatus.EMAILED, EnumSet.of(InstrumentStatus.DEBITED)));
    NEXT.put(
        DisbursementMode.CTA,
        Map.of(
            InstrumentStatus.PENDING, EnumSet.of(InstrumentStatus.EXTRACTED),
            InstrumentStatus.EXTRACTED, EnumSet.of(InstrumentStatus.CREDITED)));
    Map<InstrumentStatus, Set<InstrumentStatus>> branchForm =
        Map.of(
            InstrumentStatus.PENDING, EnumSet.of(InstrumentStatus.PRINTED),
            InstrumentStatus.PRINTED, EnumSet.of(InstrumentStatus.DEBITED));
    NEXT.put(DisbursementMode.CREDIT_TICKET, branchForm);
    NEXT.put(DisbursementMode.TT, branchForm);
    NEXT.put(
        DisbursementMode.MC_DD,
        Map.of(
            InstrumentStatus.PENDING, EnumSet.of(InstrumentStatus.PRINTED),
            InstrumentStatus.PRINTED, EnumSet.of(InstrumentStatus.RECEIVED),
            InstrumentStatus.RECEIVED, EnumSet.of(InstrumentStatus.RELEASED)));
    NEXT.put(
        DisbursementMode.ONLINE_BANKING,
        Map.of(InstrumentStatus.APPROVED, EnumSet.of(InstrumentStatus.DEBITED)));
  }

  private InstrumentLifecycle() {}

  /**
   * The status an instrument starts in when its DV is approved: online banking is APPROVED at once
   * (DIS 3.26.7), every other mode waits for its document or file.
   *
   * @param mode mode of payment
   * @return first status
   */
  public static InstrumentStatus initial(DisbursementMode mode) {
    return mode == DisbursementMode.ONLINE_BANKING
        ? InstrumentStatus.APPROVED
        : InstrumentStatus.PENDING;
  }

  /**
   * Whether a mode may move from one status to another; cancelling is allowed from every status
   * that is not final.
   *
   * @param mode mode of payment
   * @param from current status
   * @param to target status
   * @return true when allowed
   */
  public static boolean allows(DisbursementMode mode, InstrumentStatus from, InstrumentStatus to) {
    if (to == InstrumentStatus.CANCELLED) {
      return !isFinal(mode, from);
    }
    return NEXT.get(mode).getOrDefault(from, Set.of()).contains(to);
  }

  /**
   * Whether a status ends the life of the instrument (MC / DD ends when released).
   *
   * @param mode mode of payment
   * @param status status
   * @return true when final
   */
  public static boolean isFinal(DisbursementMode mode, InstrumentStatus status) {
    return FINAL.contains(status)
        || mode == DisbursementMode.MC_DD && status == InstrumentStatus.RELEASED;
  }

  /**
   * Whether the payment counts as made in this status (gateway status PAID).
   *
   * @param status status
   * @return true for released, credited, debited and negotiated
   */
  public static boolean isPaid(InstrumentStatus status) {
    return status == InstrumentStatus.RELEASED
        || status == InstrumentStatus.CREDITED
        || status == InstrumentStatus.DEBITED
        || status == InstrumentStatus.NEGOTIATED;
  }

  /**
   * Every status of a mode, in life-cycle order (for status edits, DIS 2.8.5).
   *
   * @param mode mode of payment
   * @return statuses
   */
  public static List<InstrumentStatus> statusesOf(DisbursementMode mode) {
    Set<InstrumentStatus> all = EnumSet.noneOf(InstrumentStatus.class);
    NEXT.get(mode)
        .forEach(
            (from, to) -> {
              all.add(from);
              all.addAll(to);
            });
    return List.copyOf(all);
  }
}
