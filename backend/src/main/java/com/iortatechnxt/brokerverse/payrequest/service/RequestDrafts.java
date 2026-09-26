package com.iortatechnxt.brokerverse.payrequest.service;

import com.iortatechnxt.brokerverse.payrequest.domain.RefundLineValues;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestContent;
import java.math.BigDecimal;
import java.util.List;

/** The forms of the three request kinds as entered (Appendix D, MKT 1.10.0, 1.19.0). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class RequestDrafts {

  private RequestDrafts() {}

  /**
   * Payment mode and CA / SA information (MKT 2.25.0).
   *
   * @param mode payment mode (LOV {@code PRQ_PAYMENT_MODE})
   * @param accountNo BDO account number (credit to account)
   * @param accountName account name or check name
   */
  public record Payout(String mode, String accountNo, String accountName) {}

  /**
   * A Refund Request Form (RRF).
   *
   * @param content segment, reference, requesting unit, purpose and currency
   * @param payout payment mode and account
   * @param lines one line per AR
   */
  public record Refund(RequestContent content, Payout payout, List<RefundLineValues> lines) {

    /** Defensive copy. */
    public Refund {
      lines = lines == null ? List.of() : List.copyOf(lines);
    }
  }

  /**
   * A Request for Payment of a cash advance (RFP).
   *
   * @param content RFP type, purpose, currency and requesting unit
   * @param employeeNo employee number (payee code)
   * @param employeeName employee name
   * @param payout payment mode and account
   * @param amount amount requested
   */
  public record CashAdvance(
      RequestContent content,
      String employeeNo,
      String employeeName,
      Payout payout,
      BigDecimal amount) {}

  /**
   * A request to cancel a disbursed check (MKT 1.19.0).
   *
   * @param targetRequestNo refund or cash-advance request that was paid by check
   * @param checkNo check number
   * @param reasonCode reason (LOV {@code DISB_CANCEL_REASON})
   * @param remarks remarks
   */
  public record CheckCancellation(
      String targetRequestNo, String checkNo, String reasonCode, String remarks) {}
}
