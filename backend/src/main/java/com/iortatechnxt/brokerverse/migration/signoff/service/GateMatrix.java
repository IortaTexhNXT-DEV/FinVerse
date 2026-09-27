package com.iortatechnxt.brokerverse.migration.signoff.service;

import com.iortatechnxt.brokerverse.migration.load.domain.BatchStatus;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import com.iortatechnxt.brokerverse.migration.object.domain.MigDataObject;
import com.iortatechnxt.brokerverse.migration.object.domain.MigDataObjectRepository;
import com.iortatechnxt.brokerverse.migration.signoff.domain.Gate;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoff;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoffRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The sign-off matrix (screen Sign-off; report MIG-SIGNOFF-STATUS): for each object of the register
 * and its latest batch, the state of each gate G1-G7 with who signed it and when, so the Data
 * Migration Lead sees what is missing before a trial migration or the cut-over.
 */
@Service
@Transactional(readOnly = true)
public class GateMatrix {

  private final MigDataObjectRepository objects;
  private final MigBatchRepository batches;
  private final MigSignoffRepository signoffs;

  /**
   * Creates the matrix.
   *
   * @param objects register
   * @param batches batches
   * @param signoffs sign-offs
   */
  public GateMatrix(
      MigDataObjectRepository objects, MigBatchRepository batches, MigSignoffRepository signoffs) {
    this.objects = objects;
    this.batches = batches;
    this.signoffs = signoffs;
  }

  /**
   * The matrix of a company.
   *
   * @param companyId company
   * @return one row per object
   */
  public List<Row> matrix(Long companyId) {
    List<MigSignoff> all = signoffs.findByCompanyIdOrderByIdAsc(companyId);
    Optional<MigSignoff> golive =
        all.stream().filter(s -> s.getGate() == Gate.G7).reduce((a, b) -> b);
    List<Row> out = new ArrayList<>();
    for (MigDataObject o : objects.findAllByOrderByLoadOrderAscCodeAsc()) {
      MigBatch latest =
          batches.findByCompanyIdAndObjectCodeOrderByIdDesc(companyId, o.getCode()).stream()
              .filter(b -> b.getStatus() != BatchStatus.ROLLED_BACK)
              .findFirst()
              .orElse(null);
      Map<Gate, Cell> cells = cells(all, o.getCode(), latest);
      golive.ifPresent(s -> cells.put(Gate.G7, Cell.of(s)));
      out.add(
          new Row(
              o.getCode(),
              o.getName(),
              classOf(o),
              o.getStatus().name(),
              Optional.ofNullable(latest).map(MigBatch::getBatchNo).orElse(null),
              Optional.ofNullable(latest).map(b -> b.getStatus().name()).orElse(null),
              cells));
    }
    return out;
  }

  private static String classOf(MigDataObject o) {
    return o.getDecidedClass() == null ? o.getProposedClass().name() : o.getDecidedClass().name();
  }

  /** The object gates G1-G2 and the gates of the latest batch. */
  private static Map<Gate, Cell> cells(List<MigSignoff> all, String code, MigBatch latest) {
    Map<Gate, Cell> cells = new EnumMap<>(Gate.class);
    for (MigSignoff s : all) {
      boolean objectGate = s.getGate() == Gate.G1 || s.getGate() == Gate.G2;
      boolean ofBatch = latest != null && latest.getId().equals(s.getBatchId());
      if (s.getObjectCode().equals(code) && (objectGate || ofBatch)) {
        cells.merge(s.getGate(), Cell.of(s), Cell::combine);
      }
    }
    return cells;
  }

  /**
   * A row of the matrix.
   *
   * @param objectCode object
   * @param objectName name
   * @param objectClass decided (or proposed) class
   * @param objectStatus register status
   * @param batchNo latest batch
   * @param batchStatus its status
   * @param gates gate cells
   */
  public record Row(
      String objectCode,
      String objectName,
      String objectClass,
      String objectStatus,
      String batchNo,
      String batchStatus,
      Map<Gate, Cell> gates) {}

  /**
   * A gate cell.
   *
   * @param decision APPROVED or REJECTED (the latest; APPROVED only when every role signed)
   * @param signedBy users and roles
   * @param signedAt latest time
   */
  public record Cell(String decision, String signedBy, Instant signedAt) {

    static Cell of(MigSignoff s) {
      return new Cell(
          s.getDecision().name(), s.getUsername() + " (" + s.getRoleCode() + ")", s.getSignedAt());
    }

    Cell combine(Cell later) {
      String by =
          signedBy.contains(later.signedBy()) ? signedBy : signedBy + ", " + later.signedBy();
      return new Cell(later.decision(), by, later.signedAt());
    }
  }
}
