package com.iortatechnxt.finverse.fixedasset.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.fixedasset.api.dto.DisposalRequest;
import com.iortatechnxt.finverse.fixedasset.api.dto.TransferRequest;
import com.iortatechnxt.finverse.fixedasset.domain.AssetMovement;
import com.iortatechnxt.finverse.fixedasset.domain.AssetMovementRepository;
import com.iortatechnxt.finverse.fixedasset.domain.FixedAsset;
import com.iortatechnxt.finverse.fixedasset.domain.MovementType;
import com.iortatechnxt.finverse.fixedasset.service.AssetAccounting.Posting;
import com.iortatechnxt.finverse.fixedasset.service.DepreciationCalculator.Basis;
import com.iortatechnxt.finverse.fixedasset.service.DepreciationCalculator.Position;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accounting life cycle of a registered asset: capitalization (checker; posts the acquisition or
 * the take-on opening balance), disposal with gain or loss, and inter-branch transfer.
 */
@Service
@Transactional
public class AssetLifecycleService {

  private static final String ASSET = "FixedAsset";
  private static final String COST = "COST";
  private static final String ACCUMULATED = "ACCUMULATED_DEPRECIATION";
  private static final String NET_BOOK_VALUE = "NET_BOOK_VALUE";
  private static final String TRANSFER_EVENT = "ASSET_TRANSFER";
  private static final String PREFIX = "FA:";

  private final FixedAssetService register;
  private final AssetMovementRepository movements;
  private final AssetAccounting accounting;
  private final OrganizationService organization;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param register asset register
   * @param movements movement repository
   * @param accounting accounting helper
   * @param organization organization service
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public AssetLifecycleService(
      FixedAssetService register,
      AssetMovementRepository movements,
      AssetAccounting accounting,
      OrganizationService organization,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.register = register;
    this.movements = movements;
    this.accounting = accounting;
    this.organization = organization;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Capitalizes an asset (checker): posts the acquisition, or the opening balance for a take-on.
   *
   * @param id id
   * @return asset
   */
  public FixedAsset capitalize(Long id) {
    FixedAsset a = register.get(id);
    a.capitalize(currentUser.username(), clock.instant());
    String narration = a.isTakeOn() ? "Opening balance of asset " : "Capitalization of asset ";
    Posting posting =
        a.isTakeOn()
            ? new Posting(
                "ASSET_TAKE_ON",
                a.getBranchId(),
                a.getCapitalizationDate(),
                PREFIX + a.getId() + ":TAKEON",
                a.getTagNo(),
                narration + a.getTagNo(),
                values(a),
                Map.of())
            : new Posting(
                "ASSET_ACQUISITION",
                a.getBranchId(),
                a.getCapitalizationDate(),
                PREFIX + a.getId() + ":CAP",
                a.getTagNo(),
                narration + a.getTagNo(),
                Map.of(COST, a.getAcquisitionCost()),
                Map.of("SETTLEMENT", a.getSettlementAccount()));
    JournalBatch batch = accounting.publish(a, posting);
    a.setCapitalizationBatchNo(batch.getBatchNo());
    if (!a.isTakeOn()) {
      AssetMovement addition =
          new AssetMovement(
              a, MovementType.ADDITION, a.getCapitalizationDate(), a.getBranchId(), null);
      addition.setReference(a.getSupplierCode());
      addition.setBatchNo(batch.getBatchNo());
      movements.save(addition);
    }
    audit.record(ASSET, a.getTagNo(), AuditAction.AUTHORIZE, "Capitalized, " + batch.getBatchNo());
    return a;
  }

  /**
   * Disposes of an asset: derecognizes cost and accumulated depreciation, books the proceeds and
   * the gain (proceeds above net book value) or loss. Depreciation must have been run up to the
   * month before the disposal month (no depreciation in the month of disposal).
   *
   * @param id id
   * @param r disposal
   * @return movement
   */
  public AssetMovement dispose(Long id, DisposalRequest r) {
    FixedAsset a = register.get(id);
    a.requireInService();
    requireOnOrAfterCapitalization(a, r.disposalDate());
    requireDepreciationUpTo(a, YearMonth.from(r.disposalDate()).minusMonths(1));
    boolean hasProceeds = r.proceeds().signum() > 0;
    if (hasProceeds && (r.bankAccount() == null || r.bankAccount().isBlank())) {
      throw new BusinessRuleException(
          "BANK_ACCOUNT_REQUIRED", "A bank account is required to receive the proceeds");
    }
    BigDecimal gain = r.proceeds().subtract(a.netBookValue());
    AssetMovement m =
        new AssetMovement(a, MovementType.DISPOSAL, r.disposalDate(), a.getBranchId(), null);
    m.settle(r.proceeds(), gain);
    m.setReference(r.reference());
    m.setRemarks(r.remarks());
    Map<String, BigDecimal> amounts =
        Map.of(
            COST,
            a.getAcquisitionCost(),
            ACCUMULATED,
            a.getAccumulatedDepreciation(),
            "PROCEEDS",
            r.proceeds(),
            "GAIN",
            gain.max(BigDecimal.ZERO),
            "LOSS",
            gain.negate().max(BigDecimal.ZERO));
    JournalBatch batch =
        accounting.publish(
            a,
            new Posting(
                "ASSET_DISPOSAL",
                a.getBranchId(),
                r.disposalDate(),
                PREFIX + a.getId() + ":DISPOSAL",
                r.reference() == null ? a.getTagNo() : r.reference(),
                "Disposal of asset " + a.getTagNo(),
                amounts,
                hasProceeds ? Map.of("BANK", r.bankAccount()) : Map.of()));
    m.setBatchNo(batch.getBatchNo());
    a.dispose(r.disposalDate());
    audit.record(
        ASSET, a.getTagNo(), AuditAction.UPDATE, "Disposed, gain/(loss) " + gain.toPlainString());
    return movements.save(m);
  }

  /**
   * Transfers an asset to another branch: the sending branch derecognizes the asset against
   * inter-branch clearing and the receiving branch recognizes it (two balanced journals).
   *
   * @param id id
   * @param r transfer
   * @return movement
   */
  public AssetMovement transfer(Long id, TransferRequest r) {
    FixedAsset a = register.get(id);
    a.requireInService();
    requireOnOrAfterCapitalization(a, r.transferDate());
    organization.requireActiveBranch(r.toBranchId());
    Long from = a.getBranchId();
    AssetMovement m =
        movements.save(
            new AssetMovement(a, MovementType.TRANSFER, r.transferDate(), from, r.toBranchId()));
    m.setRemarks(r.remarks());
    a.transferTo(r.toBranchId(), r.location(), r.custodian());
    String key = PREFIX + a.getId() + ":TRF:" + m.getId();
    Map<String, BigDecimal> in = values(a);
    Map<String, BigDecimal> out =
        Map.of(
            COST, in.get(COST).negate(),
            ACCUMULATED, in.get(ACCUMULATED).negate(),
            NET_BOOK_VALUE, in.get(NET_BOOK_VALUE).negate());
    String narration = "Transfer of asset " + a.getTagNo();
    JournalBatch sent =
        accounting.publish(
            a,
            new Posting(
                TRANSFER_EVENT,
                from,
                r.transferDate(),
                key + ":OUT",
                a.getTagNo(),
                narration,
                out,
                Map.of()));
    JournalBatch received =
        accounting.publish(
            a,
            new Posting(
                TRANSFER_EVENT,
                r.toBranchId(),
                r.transferDate(),
                key + ":IN",
                a.getTagNo(),
                narration,
                in,
                Map.of()));
    m.setBatchNo(sent.getBatchNo() + "/" + received.getBatchNo());
    audit.record(
        ASSET, a.getTagNo(), AuditAction.UPDATE, "Transferred to branch " + r.toBranchId());
    return m;
  }

  private static Map<String, BigDecimal> values(FixedAsset a) {
    return Map.of(
        COST, a.getAcquisitionCost(),
        ACCUMULATED, a.getAccumulatedDepreciation(),
        NET_BOOK_VALUE, a.netBookValue());
  }

  private static void requireOnOrAfterCapitalization(FixedAsset a, LocalDate date) {
    if (date.isBefore(a.getCapitalizationDate())) {
      throw new BusinessRuleException(
          "DATE_BEFORE_CAPITALIZATION",
          "Date " + date + " precedes the capitalization of asset " + a.getTagNo());
    }
  }

  private static void requireDepreciationUpTo(FixedAsset a, YearMonth month) {
    int due = (int) ChronoUnit.MONTHS.between(a.lastDepreciatedMonth(), month);
    if (due > 0
        && DepreciationCalculator.charge(Basis.of(a), Position.of(a), due).amount().signum() > 0) {
      throw new BusinessRuleException(
          "DEPRECIATION_NOT_UP_TO_DATE",
          "Run depreciation up to " + month + " before disposing of asset " + a.getTagNo());
    }
  }
}
