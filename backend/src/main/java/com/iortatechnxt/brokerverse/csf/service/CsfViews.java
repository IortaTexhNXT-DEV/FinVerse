package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.crm.service.ClientBanner;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * What the Customer Servicing Facility screens show, as plain values read inside the service
 * transaction (FR-CSF-010 to 013, 021, 030 to 033). The API serialises them as they are.
 */
public interface CsfViews {

  /**
   * The client summary card (FR-CSF-011; BRCSF-002, 008).
   *
   * @param id client id
   * @param code client or prospect code
   * @param prospectCode prospect code
   * @param name display name
   * @param clientType INDIVIDUAL or CORPORATE
   * @param status client status
   * @param kycStatus KYC status
   * @param marketSegment market segment
   * @param bankClient BDO bank client
   * @param contact contact details
   * @param banner active tags and special instructions
   * @param accounts number of accounts
   * @param verification the caller verification still valid, null when none
   */
  record ClientSummary(
      Long id,
      String code,
      String prospectCode,
      String name,
      String clientType,
      String status,
      String kycStatus,
      String marketSegment,
      boolean bankClient,
      Contact contact,
      ClientBanner banner,
      int accounts,
      VerificationView verification) {}

  /**
   * Contact details of the client master.
   *
   * @param email e-mail
   * @param mobile mobile
   * @param phone phone
   * @param addressLine address line
   * @param city city
   * @param province province
   * @param postalCode postal code
   */
  record Contact(
      String email,
      String mobile,
      String phone,
      String addressLine,
      String city,
      String province,
      String postalCode) {}

  /**
   * An account of the client with its CSF status (FR-CSF-011, 012).
   *
   * @param id account id
   * @param arn ARN
   * @param productCode product
   * @param productName product name
   * @param lineCode product line
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param stage BIBS account stage
   * @param csfStatus CSF status, null when no mapping row applies
   * @param policyNumbers policy numbers
   * @param pnNumbers PN numbers
   * @param loanApplicationNo loan application number
   * @param periodFrom period start
   * @param periodTo period end
   * @param currency currency
   * @param balance outstanding premium receivable of its invoices
   * @param paymentStatus payment status of its latest invoice, null before booking
   * @param marketSegment market segment
   * @param ffy Free First Year tagged
   * @param directPayment paid directly to the insurer
   */
  record AccountLine(
      Long id,
      String arn,
      String productCode,
      String productName,
      String lineCode,
      String insurerCode,
      String insurerName,
      String stage,
      String csfStatus,
      List<String> policyNumbers,
      List<String> pnNumbers,
      String loanApplicationNo,
      LocalDate periodFrom,
      LocalDate periodTo,
      String currency,
      BigDecimal balance,
      String paymentStatus,
      String marketSegment,
      boolean ffy,
      boolean directPayment) {

    /** Defensive copies. */
    public AccountLine {
      policyNumbers = List.copyOf(policyNumbers);
      pnNumbers = List.copyOf(pnNumbers);
    }
  }

  /**
   * A client found by a search, with the accounts that matched.
   *
   * @param id client id
   * @param code client or prospect code
   * @param name display name
   * @param clientType client type
   * @param status client status
   * @param email e-mail
   * @param mobile mobile
   * @param city city
   * @param accounts matching accounts
   */
  record ClientHitView(
      Long id,
      String code,
      String name,
      String clientType,
      String status,
      String email,
      String mobile,
      String city,
      List<AccountLine> accounts) {

    /** Defensive copy. */
    public ClientHitView {
      accounts = List.copyOf(accounts);
    }
  }

  /**
   * A search with its results.
   *
   * @param keyType key type
   * @param value searched value
   * @param truncated more clients matched than returned
   * @param max maximum returned
   * @param clients clients found
   * @param legacy accounts found only in the legacy systems
   */
  record SearchView(
      String keyType,
      String value,
      boolean truncated,
      int max,
      List<ClientHitView> clients,
      List<LegacyAccountView> legacy) {

    /** Defensive copies. */
    public SearchView {
      clients = List.copyOf(clients);
      legacy = List.copyOf(legacy);
    }
  }

  /**
   * An account found only in a legacy system.
   *
   * @param source legacy system
   * @param reference legacy reference
   * @param clientName client name
   * @param description description
   */
  record LegacyAccountView(
      String source, String reference, String clientName, String description) {}

  /**
   * A payment of the client with the invoices it was applied to (FR-CSF-013).
   *
   * @param receiptNo official or acknowledgement receipt number, or the application reference
   * @param orNo official receipt
   * @param arNo acknowledgement receipt
   * @param valueDate value date
   * @param mode mode of payment, null when not known
   * @param kind PAYMENT, REVERSAL or LEGACY
   * @param amount amount applied (negative for a reversal)
   * @param applications invoices paid
   */
  record PaymentView(
      String receiptNo,
      String orNo,
      String arNo,
      LocalDate valueDate,
      String mode,
      String kind,
      BigDecimal amount,
      List<ApplicationView> applications) {

    /** Defensive copy. */
    public PaymentView {
      applications = List.copyOf(applications);
    }
  }

  /**
   * One invoice paid by a payment, with its current balance.
   *
   * @param invoiceNo invoice
   * @param arn account
   * @param amount amount applied
   * @param invoiceBalance current premium balance of the invoice
   * @param invoicePaymentStatus current payment status of the invoice
   */
  record ApplicationView(
      String invoiceNo,
      String arn,
      BigDecimal amount,
      BigDecimal invoiceBalance,
      String invoicePaymentStatus) {}

  /**
   * The payment history of a window.
   *
   * @param from first value date
   * @param months months shown
   * @param payments payments, newest first
   */
  record PaymentHistory(LocalDate from, int months, List<PaymentView> payments) {

    /** Defensive copy. */
    public PaymentHistory {
      payments = List.copyOf(payments);
    }
  }

  /**
   * A document of the client's records (FR-CSF-030, 033).
   *
   * @param id attachment id
   * @param fileName file name
   * @param documentType document type
   * @param contentType content type
   * @param sizeBytes size
   * @param uploadedBy user
   * @param uploadedAt time
   * @param recordType Client, Account or Quotation
   * @param reference client code, ARN or quotation number
   */
  record DocumentView(
      Long id,
      String fileName,
      String documentType,
      String contentType,
      long sizeBytes,
      String uploadedBy,
      Instant uploadedAt,
      String recordType,
      String reference) {}

  /**
   * An e-policy of the client's accounts (FR-CSF-031).
   *
   * @param id e-policy id
   * @param arn account
   * @param fileName file name
   * @param policyNumbers policy numbers
   * @param status e-policy status
   * @param receivedAt time received
   * @param dispatchCount times sent
   * @param lastSentTo last recipients
   * @param lastSentAt last sending
   * @param resendable confirmed, so it may be resent
   */
  record EpolicyView(
      Long id,
      String arn,
      String fileName,
      List<String> policyNumbers,
      String status,
      Instant receivedAt,
      int dispatchCount,
      String lastSentTo,
      Instant lastSentAt,
      boolean resendable) {

    /** Defensive copy. */
    public EpolicyView {
      policyNumbers = List.copyOf(policyNumbers);
    }
  }

  /**
   * A caller verification (FR-CSF-020).
   *
   * @param id verification id
   * @param channel channel
   * @param result PASSED or FAILED
   * @param matches checks matched
   * @param required checks required
   * @param checks each check and whether it matched
   * @param verifiedAt time
   * @param validUntil end of validity of a pass
   * @param agent agent
   */
  record VerificationView(
      Long id,
      String channel,
      String result,
      int matches,
      int required,
      List<CheckView> checks,
      Instant verifiedAt,
      Instant validUntil,
      String agent) {

    /** Defensive copy. */
    public VerificationView {
      checks = List.copyOf(checks);
    }
  }

  /**
   * One verification check.
   *
   * @param code check (list CSF_VERIFY_CHECK)
   * @param matched answer matched
   */
  record CheckView(String code, boolean matched) {}

  /**
   * A contact change, refused change or referral (FR-CSF-021, 022; Contact History).
   *
   * @param id change id
   * @param changeNo change number
   * @param clientId client
   * @param clientCode client code
   * @param clientName client name
   * @param status APPLIED, REFUSED or REFERRED
   * @param at time
   * @param agent agent
   * @param channel channel
   * @param reasonCode reason
   * @param remarks remarks
   * @param verificationResult result of the verification used, null for a referral
   * @param verificationMatches checks matched, null for a referral
   * @param syncStatus legacy write-back state
   * @param handoffStatus state of the Operations hand-off of a referral
   * @param fields field rows
   */
  record ChangeView(
      Long id,
      String changeNo,
      Long clientId,
      String clientCode,
      String clientName,
      String status,
      Instant at,
      String agent,
      String channel,
      String reasonCode,
      String remarks,
      String verificationResult,
      Integer verificationMatches,
      String syncStatus,
      String handoffStatus,
      List<FieldView> fields) {

    /** Defensive copy. */
    public ChangeView {
      fields = List.copyOf(fields);
    }
  }

  /**
   * One field of a change.
   *
   * @param field field code
   * @param oldValue value before
   * @param newValue value after or asked for
   */
  record FieldView(String field, String oldValue, String newValue) {}

  /**
   * The e-mail a resend will send (Resend dialog preview, FR-CSF-030, 031).
   *
   * @param documentName document
   * @param registeredEmail registered e-mail of the client, null when none
   * @param subject subject
   * @param body body
   * @param otherAllowed the user may send to another address
   */
  record ResendPreview(
      String documentName,
      String registeredEmail,
      String subject,
      String body,
      boolean otherAllowed) {}

  /**
   * A resend done.
   *
   * @param recipient recipient
   * @param messageId outbox message, null for an e-policy (sent by Issuance)
   * @param documentName document
   */
  record ResendResult(String recipient, Long messageId, String documentName) {}
}
