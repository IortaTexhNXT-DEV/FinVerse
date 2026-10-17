package com.iortatechnxt.brokerverse.report.core;

import java.util.List;
import java.util.Optional;

/**
 * The data elements an MIS field may read (FR-CR-062, "Source"): each belongs to a core entity and
 * is carried by report columns under one of a few keys. The catalogue names these elements; it
 * never creates data (CRQ09), so every element here maps to data BIBS already holds.
 */
public enum MisDataElement {
  /** Product type or line of business. */
  PRODUCT_TYPE(MisEntity.PRODUCT, "product", "product_type", "productType", "product_line", "lob"),
  /** Risk code. */
  RISK_CODE(MisEntity.PRODUCT, "risk_code", "riskCode"),
  /** Insurer. */
  INSURER(MisEntity.INSURER, "insurer", "insurer_code", "insurerCode", "insurer_name"),
  /** Policy number. */
  POLICY_NUMBER(MisEntity.ACCOUNT, "policy_no", "policyNo", "policy_number"),
  /** Market segment. */
  MARKET_SEGMENT(MisEntity.ACCOUNT, "segment", "market_segment", "marketSegment"),
  /** Booking date. */
  BOOKING_DATE(MisEntity.ACCOUNT, "booking_date", "bookingDate"),
  /** Branch. */
  BRANCH(MisEntity.ACCOUNT, "branch", "branch_code", "branchCode", "branch_name"),
  /** Status of the record. */
  STATUS(MisEntity.ACCOUNT, "status", "stage"),
  /** Business origin. */
  BUSINESS_ORIGIN(MisEntity.ACCOUNT, "business_origin", "businessOrigin"),
  /** Region of the branch. */
  REGION(MisEntity.ACCOUNT, "region"),
  /** Area of the branch. */
  AREA(MisEntity.ACCOUNT, "area"),
  /** Claim status. */
  CLAIM_STATUS(MisEntity.CLAIM, "claim_status", "claimStatus"),
  /** Account officer. */
  ACCOUNT_OFFICER(MisEntity.USER, "account_officer", "accountOfficer", "ao"),
  /** Unit head. */
  UNIT_HEAD(MisEntity.USER, "unit_head", "unitHead"),
  /** Department. */
  DEPARTMENT(MisEntity.USER, "department"),
  /** Transaction type. */
  TRANSACTION_TYPE(MisEntity.TRANSACTION, "transaction_type", "transactionType"),
  /** Account of the chart of accounts. */
  CHART_OF_ACCOUNT(MisEntity.TRANSACTION, "gl_account", "glAccount", "account_code");

  /** The core entities of the MIS fields. */
  public enum MisEntity {
    /** Users. */
    USER,
    /** Products. */
    PRODUCT,
    /** Insurers. */
    INSURER,
    /** Claims. */
    CLAIM,
    /** Policies and accounts. */
    ACCOUNT,
    /** Transactions. */
    TRANSACTION
  }

  private final MisEntity entity;
  private final List<String> columnKeys;

  MisDataElement(MisEntity entity, String... columnKeys) {
    this.entity = entity;
    this.columnKeys = List.of(columnKeys);
  }

  /**
   * The entity of the element.
   *
   * @return entity
   */
  public MisEntity entity() {
    return entity;
  }

  /**
   * The report column keys that carry the element.
   *
   * @return keys
   */
  public List<String> columnKeys() {
    return columnKeys;
  }

  /**
   * The element a report column carries.
   *
   * @param columnKey column key
   * @return element, empty when the column carries none
   */
  public static Optional<MisDataElement> ofColumn(String columnKey) {
    for (MisDataElement e : values()) {
      if (e.columnKeys.contains(columnKey)) {
        return Optional.of(e);
      }
    }
    return Optional.empty();
  }
}
