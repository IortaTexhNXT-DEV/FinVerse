package com.iortatechnxt.brokerverse.eb.member.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.eb.domain.EbMember;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChange;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbRosterVersion;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Checks of the lines of a member change against the accepted roster (FR-EB-055): a deletion or
 * change names an active member of the roster, an addition a new employee number; additions and
 * data changes carry the member data, plan changes the plan; effective dates fall within the
 * line's policy period.
 */
@Component
public class MemberChangeRules {

  private final EbMemberRepository members;

  /**
   * Creates the checks.
   *
   * @param members roster members
   */
  public MemberChangeRules(EbMemberRepository members) {
    this.members = members;
  }

  /**
   * Checks a line and resolves its roster member.
   *
   * @param roster accepted roster
   * @param line programme line (policy period)
   * @param input line
   * @param seen employee numbers already in the change
   * @return line data with the member
   */
  EbMemberChange.LineData check(
      EbRosterVersion roster, EbProgrammeLine line, MemberChangeInput.Line input, Set<String> seen) {
    if (input.action() == null || blank(input.employeeNo())) {
      throw new BusinessRuleException(
          "EB_MEMBER_LINE_REQUIRED", "Select the action and enter the employee number of each line");
    }
    String no = input.employeeNo().strip();
    if (!seen.add(no.toUpperCase(Locale.ROOT))) {
      throw new BusinessRuleException(
          "EB_MEMBER_TWICE", "Employee " + no + " appears twice in the member change");
    }
    effective(line, input.effectiveDate());
    Optional<EbMember> member =
        members
            .findByRosterVersionIdAndEmployeeNoIgnoreCase(roster.getId(), no)
            .filter(m -> m.getStatus() == EbMember.Status.ACTIVE);
    return input.action() == EbMemberChange.Action.ADD
        ? added(no, input, member)
        : changed(no, input, member);
  }

  private static EbMemberChange.LineData added(
      String no, MemberChangeInput.Line input, Optional<EbMember> member) {
    if (member.isPresent()) {
      throw new BusinessRuleException(
          "EB_MEMBER_EXISTS", "Employee " + no + " is already on the roster");
    }
    requireData(no, input.member());
    return new EbMemberChange.LineData(
        input.action(), no, input.member(), input.effectiveDate(), null);
  }

  private static EbMemberChange.LineData changed(
      String no, MemberChangeInput.Line input, Optional<EbMember> member) {
    EbMember existing =
        member.orElseThrow(
            () ->
                new BusinessRuleException(
                    "EB_MEMBER_UNKNOWN", "Employee " + no + " is not on the roster"));
    EbMember.Data data = null;
    if (input.action() == EbMemberChange.Action.CHANGE_DATA) {
      requireData(no, input.member());
      data = input.member();
    } else if (input.action() == EbMemberChange.Action.CHANGE_PLAN) {
      if (input.member() == null || blank(input.member().planCode())) {
        throw new BusinessRuleException("EB_MEMBER_PLAN", "Enter the new plan of employee " + no);
      }
      data = withPlan(existing, input.member().planCode().strip());
    }
    return new EbMemberChange.LineData(
        input.action(), no, data, input.effectiveDate(), existing.getId());
  }

  private static EbMember.Data withPlan(EbMember existing, String plan) {
    return new EbMember.Data(
        existing.getLastName(),
        existing.getFirstName(),
        existing.getBirthDate(),
        existing.getGender(),
        existing.getCivilStatus(),
        plan,
        existing.getDependants());
  }

  private static void effective(EbProgrammeLine line, LocalDate date) {
    if (date == null) {
      throw new BusinessRuleException("EB_MEMBER_EFFECTIVE", "Enter the effective date of each line");
    }
    boolean before = line.getPeriodFrom() != null && date.isBefore(line.getPeriodFrom());
    boolean after = line.getPeriodTo() != null && date.isAfter(line.getPeriodTo());
    if (before || after) {
      throw new BusinessRuleException(
          "EB_MEMBER_EFFECTIVE_PERIOD", "The effective date must be within the policy period");
    }
  }

  private static void requireData(String no, EbMember.Data data) {
    if (data == null
        || blank(data.lastName())
        || blank(data.firstName())
        || data.birthDate() == null
        || blank(data.planCode())) {
      throw new BusinessRuleException(
          "EB_MEMBER_DATA", "Enter the name, birth date and plan of employee " + no);
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  /**
   * A fresh set of employee numbers.
   *
   * @return set
   */
  static Set<String> seen() {
    return new HashSet<>();
  }
}
