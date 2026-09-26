package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.SalesLevel;
import com.iortatechnxt.brokerverse.catalog.domain.SalesOfficer;
import com.iortatechnxt.brokerverse.catalog.domain.SalesOfficerRepository;
import com.iortatechnxt.brokerverse.catalog.domain.SalesUnit;
import com.iortatechnxt.brokerverse.catalog.domain.SalesUnit.UnitDetails;
import com.iortatechnxt.brokerverse.catalog.domain.SalesUnitRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.dimension.domain.DimensionType;
import com.iortatechnxt.brokerverse.dimension.service.DimensionService;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sales organisation (BRNB.075/108): regions, departments and teams with a default cost center per
 * team, and the account officers of each team. Accounts take their sales unit and cost center from
 * here when they are created (Q34/Q41 parked: BDOI's hierarchy and cost center rule).
 */
@Service
@Transactional
public class SalesOrganisationService {

  private final SalesUnitRepository units;
  private final SalesOfficerRepository officers;
  private final DimensionService dimensions;
  private final AppUserRepository users;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param units sales units
   * @param officers account officers
   * @param dimensions dimensions (cost centers)
   * @param users users
   * @param audit audit trail
   */
  public SalesOrganisationService(
      SalesUnitRepository units,
      SalesOfficerRepository officers,
      DimensionService dimensions,
      AppUserRepository users,
      AuditTrailService audit) {
    this.units = units;
    this.officers = officers;
    this.dimensions = dimensions;
    this.users = users;
    this.audit = audit;
  }

  /**
   * Units of a company.
   *
   * @param companyId company
   * @return units
   */
  @Transactional(readOnly = true)
  public List<SalesUnit> units(Long companyId) {
    return units.findByCompanyIdOrderByLevelAscCodeAsc(companyId);
  }

  /**
   * Account officers of a company.
   *
   * @param companyId company
   * @return officers
   */
  @Transactional(readOnly = true)
  public List<SalesOfficer> officers(Long companyId) {
    return officers.findByCompanyIdOrderByTeamCodeAscUsernameAsc(companyId);
  }

  /**
   * Adds a unit, pending authorization.
   *
   * @param companyId company
   * @param level level
   * @param code code
   * @param details name, parent and cost center
   * @return unit
   */
  public SalesUnit createUnit(Long companyId, SalesLevel level, String code, UnitDetails details) {
    if (units.findByCompanyIdAndCode(companyId, code).isPresent()) {
      throw new DuplicateResourceException(CatalogKind.SALES_UNIT.label(), code);
    }
    validate(companyId, level, details);
    SalesUnit saved = units.save(new SalesUnit(companyId, level, code, details));
    audit.record(
        CatalogKind.SALES_UNIT.label(), code, AuditAction.CREATE, saved.catalogDescription());
    return saved;
  }

  /**
   * Changes a unit, pending authorization.
   *
   * @param id unit
   * @param details name, parent and cost center
   * @return unit
   */
  public SalesUnit updateUnit(Long id, UnitDetails details) {
    SalesUnit unit =
        units
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(CatalogKind.SALES_UNIT.label(), id));
    validate(unit.getCompanyId(), unit.getLevel(), details);
    unit.update(details);
    audit.record(
        CatalogKind.SALES_UNIT.label(),
        unit.getCode(),
        AuditAction.UPDATE,
        unit.catalogDescription());
    return unit;
  }

  private void validate(Long companyId, SalesLevel level, UnitDetails details) {
    if (level != SalesLevel.REGION) {
      SalesLevel parentLevel = level == SalesLevel.TEAM ? SalesLevel.DEPARTMENT : SalesLevel.REGION;
      boolean parentOk =
          details.parentCode() != null
              && units
                  .findByCompanyIdAndCode(companyId, details.parentCode())
                  .filter(p -> p.getLevel() == parentLevel)
                  .isPresent();
      if (!parentOk) {
        throw new BusinessRuleException(
            "SALES_PARENT_INVALID", "A " + level + " must belong to a " + parentLevel);
      }
    }
    dimensions.validateOptional(companyId, DimensionType.COST_CENTER, details.costCenter());
  }

  /**
   * Places an account officer in a team (or moves them), pending authorization.
   *
   * @param companyId company
   * @param teamCode team
   * @param username user
   * @return officer
   */
  public SalesOfficer assignOfficer(Long companyId, String teamCode, String username) {
    units
        .findByCompanyIdAndCode(companyId, teamCode)
        .filter(u -> u.getLevel() == SalesLevel.TEAM)
        .orElseThrow(
            () -> new BusinessRuleException("SALES_TEAM_UNKNOWN", "Unknown team " + teamCode));
    if (!users.existsByUsernameIgnoreCase(username)) {
      throw new BusinessRuleException("USER_UNKNOWN", "Unknown user " + username);
    }
    Optional<SalesOfficer> existing = officers.findByCompanyIdAndUsername(companyId, username);
    SalesOfficer officer;
    if (existing.isPresent()) {
      officer = existing.get();
      officer.moveTo(teamCode);
    } else {
      officer = officers.save(new SalesOfficer(companyId, teamCode, username));
    }
    audit.record(
        CatalogKind.SALES_OFFICER.label(),
        username,
        existing.isPresent() ? AuditAction.UPDATE : AuditAction.CREATE,
        officer.catalogDescription());
    return officer;
  }

  /**
   * Sales unit and default cost center of an account officer, from the authorized organisation.
   *
   * @param companyId company
   * @param username account officer
   * @return region, department, team and cost center; empty when the user is in no team
   */
  @Transactional(readOnly = true)
  public Optional<SalesAssignment> assignmentOf(Long companyId, String username) {
    return officers
        .findByCompanyIdAndUsername(companyId, username)
        .filter(SalesOfficer::isActive)
        .flatMap(o -> activeUnit(companyId, o.getTeamCode()))
        .map(
            team -> {
              Optional<SalesUnit> department = activeUnit(companyId, team.getParentCode());
              String region = department.map(SalesUnit::getParentCode).orElse(null);
              return new SalesAssignment(
                  region,
                  department.map(SalesUnit::getCode).orElse(null),
                  team.getCode(),
                  team.getCostCenter());
            });
  }

  /**
   * The Unit Head of a sales unit (BRCLXN.011/012, CQ05): the head of the unit itself, else of its
   * department, else of its region.
   *
   * @param companyId company
   * @param unitCode sales unit (usually a team), may be null
   * @return head user name; empty when no unit on the way up has a head
   */
  @Transactional(readOnly = true)
  public Optional<String> unitHead(Long companyId, String unitCode) {
    String code = unitCode;
    for (int level = 0; code != null && level < SalesLevel.values().length; level++) {
      Optional<SalesUnit> unit = units.findByCompanyIdAndCode(companyId, code);
      if (unit.isEmpty()) {
        return Optional.empty();
      }
      if (unit.get().getHeadUsername() != null) {
        return Optional.of(unit.get().getHeadUsername());
      }
      code = unit.get().getParentCode();
    }
    return Optional.empty();
  }

  /**
   * Sets or clears the Unit Head of a sales unit (BRCLXN.011/012), audited; the unit keeps its
   * authorization status (operational attribute).
   *
   * @param companyId company
   * @param unitCode sales unit
   * @param username head, null or blank to clear
   * @return the unit
   */
  public SalesUnit assignHead(Long companyId, String unitCode, String username) {
    SalesUnit unit =
        units
            .findByCompanyIdAndCode(companyId, unitCode)
            .orElseThrow(
                () -> new ResourceNotFoundException(CatalogKind.SALES_UNIT.label(), unitCode));
    String head = username == null || username.isBlank() ? null : username.strip();
    if (head != null && !users.existsByUsernameIgnoreCase(head)) {
      throw new BusinessRuleException("USER_UNKNOWN", "Unknown user " + head);
    }
    String previous = unit.getHeadUsername();
    unit.assignHead(head);
    audit.record(
        CatalogKind.SALES_UNIT.label(),
        unitCode,
        AuditAction.UPDATE,
        "Unit head changed from " + previous + " to " + head);
    return unit;
  }

  private Optional<SalesUnit> activeUnit(Long companyId, String code) {
    return code == null
        ? Optional.empty()
        : units.findByCompanyIdAndCode(companyId, code).filter(SalesUnit::isActive);
  }

  /**
   * Where an account officer sits in the sales organisation.
   *
   * @param region region code
   * @param department department code
   * @param team team code
   * @param costCenter default cost center of the team
   */
  public record SalesAssignment(String region, String department, String team, String costCenter) {}
}
