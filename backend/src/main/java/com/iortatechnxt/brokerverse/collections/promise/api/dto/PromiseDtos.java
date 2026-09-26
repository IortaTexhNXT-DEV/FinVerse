package com.iortatechnxt.brokerverse.collections.promise.api.dto;

import com.iortatechnxt.brokerverse.collections.promise.domain.PaymentPromise;
import com.iortatechnxt.brokerverse.collections.promise.domain.PromiseStatus;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseService.PromiseInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Requests and responses of the promise to pay endpoints (BRCLXN.055). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class PromiseDtos {

  private PromiseDtos() {}

  /**
   * A promise to pay.
   *
   * @param id id
   * @param invoiceNo invoice
   * @param arn account
   * @param clientCode client
   * @param assuredName assured
   * @param installmentId installment, may be null
   * @param promisedOn day of the promise
   * @param promisedDate promised payment date
   * @param promisedAmount promised amount
   * @param currency currency
   * @param remarks remarks
   * @param status status
   * @param evaluatedAt evaluation time
   * @param actualPaid payments found in the window
   * @param actualDate last payment date
   * @param closingNote cancellation note
   * @param bulkRef bulk reference
   * @param recordedBy collector
   * @param createdAt recorded
   */
  public record PromiseResponse(
      Long id,
      String invoiceNo,
      String arn,
      String clientCode,
      String assuredName,
      Long installmentId,
      LocalDate promisedOn,
      LocalDate promisedDate,
      BigDecimal promisedAmount,
      String currency,
      String remarks,
      PromiseStatus status,
      Instant evaluatedAt,
      BigDecimal actualPaid,
      LocalDate actualDate,
      String closingNote,
      String bulkRef,
      String recordedBy,
      Instant createdAt) {

    /**
     * Maps a promise.
     *
     * @param p promise
     * @return DTO
     */
    public static PromiseResponse from(PaymentPromise p) {
      return new PromiseResponse(
          p.getId(),
          p.getInvoiceNo(),
          p.getArn(),
          p.getClientCode(),
          p.getAssuredName(),
          p.getInstallmentId(),
          p.getPromisedOn(),
          p.getPromisedDate(),
          p.getPromisedAmount(),
          p.getCurrency(),
          p.getRemarks(),
          p.getStatus(),
          p.getEvaluatedAt(),
          p.getActualPaid(),
          p.getActualDate(),
          p.getClosingNote(),
          p.getBulkRef(),
          p.getCreatedBy(),
          p.getCreatedAt());
    }
  }

  /**
   * A new promise.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param promisedOn day of the promise (today when empty)
   * @param promisedDate promised payment date
   * @param amount amount (whole outstanding when empty)
   * @param installmentId installment, may be null
   * @param remarks remarks
   */
  public record PromiseRequest(
      @NotNull Long companyId,
      @NotBlank @Size(max = 40) String invoiceNo,
      LocalDate promisedOn,
      @NotNull LocalDate promisedDate,
      @Positive BigDecimal amount,
      Long installmentId,
      @Size(max = 500) String remarks) {

    /**
     * The service input.
     *
     * @return input
     */
    public PromiseInput toInput() {
      return new PromiseInput(promisedOn, promisedDate, amount, installmentId, remarks);
    }
  }
}
