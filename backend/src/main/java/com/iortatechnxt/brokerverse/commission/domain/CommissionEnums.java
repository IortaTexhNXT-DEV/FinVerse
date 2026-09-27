package com.iortatechnxt.brokerverse.commission.domain;

import java.util.List;

/** Enumerations of commission receivables (CMRID.001-015). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class CommissionEnums {

  private CommissionEnums() {}

  /** Where a DP list comes from (CMRID.001). */
  public enum ListSource {
    /** A branch's Marketing Collection. */
    BRANCH,
    /** Head Office Marketing Collection. */
    HEAD_OFFICE,
    /** The Collection system feed (parked, OQ38). */
    COLLECTION_FEED
  }

  /** Tag of a direct payment account (CMRID.008, BRD 6.8). */
  public enum DpTag {
    /** Valid, waiting for the reviewer's confirmation that it is fully paid to the insurer. */
    DP_FOR_CONFIRMATION,
    /** Confirmed, to be billed to the insurer. */
    DP_FOR_BILLING,
    /** Billed, waiting for the insurer's answer. */
    BILLED,
    /** Approved by the insurer, commission to collect. */
    APPROVED,
    /** Rejected by the insurer, returned to Collection. */
    REJECTED,
    /** Commission collected (OR issued or handed over). */
    COLLECTED,
    /** Premium receivable reversed (MKTID.012). */
    PR_REVERSED,
    /** Removed by the sanitation (duplicate, invalid, incomplete) or by hand. */
    EXCLUDED;

    /** Tags of accounts still in the commission cycle (duplicates are checked against them). */
    public static final List<DpTag> ACTIVE =
        List.of(DP_FOR_CONFIRMATION, DP_FOR_BILLING, BILLED, APPROVED, COLLECTED, PR_REVERSED);
  }

  /** Sanitation result of a DP account (CMRID.013). */
  public enum Sanitation {
    /** Passed every rule. */
    VALID,
    /** Already listed (same invoice in another list or branch). */
    DUPLICATE,
    /** A validation rule failed. */
    INVALID,
    /** A mandatory field is missing. */
    INCOMPLETE
  }

  /** Incentive scheme types of the BRD (CMRID.005/006). */
  public enum SchemeType {
    /** No Touch. */
    NO_TOUCH,
    /** Top Up. */
    TOP_UP,
    /** Motor Mania. */
    MOTOR_MANIA,
    /** Any other scheme. */
    OTHER
  }

  /** How a scheme computes the incentive. */
  public enum Calculation {
    /** Production against target tiers, rate times multiplier (No Touch, Top Up). */
    TARGET_TIERED,
    /** Fixed amount per policy by minimum basic premium (Motor Mania). */
    FIXED_PER_POLICY
  }

  /** Period of a scheme. */
  public enum PeriodType {
    /** Monthly. */
    MONTHLY,
    /** Quarterly. */
    QUARTERLY,
    /** Half-yearly (e.g. June-December). */
    SEMI_ANNUAL,
    /** Yearly. */
    ANNUAL,
    /** Any dates. */
    CUSTOM
  }

  /** Who keeps the incentive. */
  public enum Beneficiary {
    /** BDOI keeps it. */
    BDOI,
    /** Passed on to the branches (Disbursement). */
    BRANCH
  }

  /** Status of an incentive run. */
  public enum RunStatus {
    /** Computed, to review. */
    COMPUTED,
    /** Accrued (and passed on). */
    POSTED,
    /** Cancelled before posting. */
    CANCELLED
  }
}
