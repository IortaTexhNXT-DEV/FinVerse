package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.catalog.domain.SalesLevel;
import com.iortatechnxt.brokerverse.catalog.domain.SalesOfficer;
import com.iortatechnxt.brokerverse.catalog.domain.SalesUnit;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.security.service.UserDirectoryService;
import com.iortatechnxt.brokerverse.security.service.UserDirectoryService.Entry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The sales organisation as an Excel file: one row per unit in tree order (region, its departments,
 * their teams) followed by the team's account officers, with the effective cost center and the
 * officers by name and role (never the login id).
 */
@Service
@Transactional(readOnly = true)
public class SalesOrganisationExport {

  private static final List<String> HEADERS =
      List.of(
          "Region",
          "Department",
          "Team",
          "Level",
          "Unit Code",
          "Unit Name",
          "Cost Center",
          "Cost Center Source",
          "Account Officer",
          "Position",
          "Assigned Since",
          "Status",
          "Status Reason");

  /** Region, department and team columns. */
  private static final int LEVELS = SalesLevel.values().length;

  private final SalesOrganisationService sales;
  private final UserDirectoryService directory;
  private final DocumentComposer composer;

  /**
   * Creates the export.
   *
   * @param sales sales organisation
   * @param directory user directory (names and roles)
   * @param composer spreadsheet composer
   */
  public SalesOrganisationExport(
      SalesOrganisationService sales, UserDirectoryService directory, DocumentComposer composer) {
    this.sales = sales;
    this.directory = directory;
    this.composer = composer;
  }

  /**
   * The organisation of a company as an Excel file.
   *
   * @param companyId company
   * @return XLSX file
   */
  public byte[] xlsx(Long companyId) {
    return composer.xlsx(new SheetSpec("Sales Organisation", HEADERS, rows(companyId)));
  }

  /**
   * The rows of the export, in tree order.
   *
   * @param companyId company
   * @return rows
   */
  public List<List<Object>> rows(Long companyId) {
    List<SalesUnit> units = sales.units(companyId);
    Map<String, Entry> people =
        directory.entries().stream()
            .collect(
                Collectors.toMap(
                    e -> e.username().toLowerCase(Locale.ROOT), Function.identity(), (a, b) -> a));
    Tree tree = new Tree(units, sales.officers(companyId), people, new ArrayList<>());
    Set<String> codes = units.stream().map(SalesUnit::getCode).collect(Collectors.toSet());
    units.stream()
        .filter(u -> u.getParentCode() == null || !codes.contains(u.getParentCode()))
        .forEach(u -> add(tree, u, new Branch(Arrays.asList(new String[LEVELS]), null, null)));
    return tree.rows();
  }

  private static void add(Tree tree, SalesUnit unit, Branch parent) {
    String[] codes = parent.path().toArray(new String[0]);
    codes[unit.getLevel().ordinal()] = unit.getCode();
    List<String> path = Arrays.asList(codes);
    boolean own = unit.getCostCenter() != null;
    Branch here =
        own
            ? new Branch(path, unit.getCostCenter(), unit.getCode())
            : new Branch(path, parent.costCenter(), parent.costCenterOf());
    String source = null;
    if (own) {
      source = "Own";
    } else if (here.costCenter() != null) {
      source = "Inherited from " + here.costCenterOf();
    }
    List<Object> unitRow = new ArrayList<>(path);
    unitRow.addAll(
        Arrays.asList(
            level(unit.getLevel()),
            unit.getCode(),
            unit.getName(),
            here.costCenter(),
            source,
            null,
            null,
            null,
            status(unit.getRecordStatus()),
            unit.getStatusReason()));
    tree.rows().add(unitRow);
    tree.officers().stream()
        .filter(o -> unit.getCode().equals(o.getTeamCode()))
        .forEach(o -> tree.rows().add(officerRow(tree, unit, here, o)));
    tree.units().stream()
        .filter(u -> unit.getCode().equals(u.getParentCode()))
        .forEach(u -> add(tree, u, here));
  }

  private static List<Object> officerRow(
      Tree tree, SalesUnit team, Branch branch, SalesOfficer officer) {
    Entry person = tree.people().get(officer.getUsername().toLowerCase(Locale.ROOT));
    List<Object> row = new ArrayList<>(branch.path());
    row.addAll(
        Arrays.asList(
            "Account Officer",
            team.getCode(),
            team.getName(),
            branch.costCenter(),
            null,
            person == null ? officer.getUsername() : person.displayName(),
            person == null ? null : person.roleName(),
            officer.getAssignedSince(),
            status(officer.getRecordStatus()),
            officer.getStatusReason()));
    return row;
  }

  private static String level(SalesLevel level) {
    return switch (level) {
      case REGION -> "Region";
      case DEPARTMENT -> "Department";
      case TEAM -> "Team";
    };
  }

  private static String status(RecordStatus status) {
    return switch (status) {
      case ACTIVE -> "Active";
      case INACTIVE -> "Inactive";
      case PENDING_AUTHORIZATION -> "Pending Authorization";
    };
  }

  /** What the walk over the tree reads and writes. */
  private record Tree(
      List<SalesUnit> units,
      List<SalesOfficer> officers,
      Map<String, Entry> people,
      List<List<Object>> rows) {}

  /**
   * Where a unit sits: its region / department / team codes and the effective cost center with the
   * unit that sets it.
   */
  private record Branch(List<String> path, String costCenter, String costCenterOf) {}
}
