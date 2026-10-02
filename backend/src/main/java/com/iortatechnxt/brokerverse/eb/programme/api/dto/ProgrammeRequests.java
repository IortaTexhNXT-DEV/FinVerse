package com.iortatechnxt.brokerverse.eb.programme.api.dto;

import com.iortatechnxt.brokerverse.eb.domain.EbContactRole;
import com.iortatechnxt.brokerverse.eb.domain.EbFunding;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeContact;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeInput;
import com.iortatechnxt.brokerverse.eb.programme.service.ProgrammeQuery;
import java.time.LocalDate;
import java.util.List;

/** Request bodies of the programme API (FR-EB-021, FR-EB-022). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class ProgrammeRequests {

  private ProgrammeRequests() {}

  /**
   * A new programme.
   *
   * @param clientId client
   * @param profile name, team, funding, AO, sales unit, renewal flag
   * @param lines benefit lines
   * @param contacts HR contacts
   */
  public record ProgrammeRequest(
      Long clientId,
      ProfileRequest profile,
      List<LineRequest> lines,
      List<ContactRequest> contacts) {

    /**
     * The service input.
     *
     * @return input
     */
    public ProgrammeInput toInput() {
      ProfileRequest p = profile == null ? ProfileRequest.EMPTY : profile;
      return new ProgrammeInput(
          clientId,
          p.name(),
          p.teamCode(),
          p.funding(),
          p.accountOfficer(),
          p.salesUnit(),
          Boolean.TRUE.equals(p.renewalEligible()),
          lines == null ? List.of() : lines.stream().map(LineRequest::toData).toList(),
          contacts == null ? List.of() : contacts.stream().map(ContactRequest::toData).toList());
    }
  }

  /**
   * The maintainable data of a programme.
   *
   * @param name programme name
   * @param teamCode team (list EB_TEAM)
   * @param funding EMPLOYER or VOLUNTARY
   * @param accountOfficer AO user name; the current user (new) or unchanged when blank
   * @param salesUnit sales unit
   * @param renewalEligible eligible for renewal
   */
  public record ProfileRequest(
      String name,
      String teamCode,
      EbFunding funding,
      String accountOfficer,
      String salesUnit,
      Boolean renewalEligible) {

    static final ProfileRequest EMPTY = new ProfileRequest(null, null, null, null, null, null);

    /**
     * The domain profile.
     *
     * @return profile
     */
    public EbProgramme.Profile toProfile() {
      return new EbProgramme.Profile(
          name, teamCode, funding, accountOfficer, salesUnit, Boolean.TRUE.equals(renewalEligible));
    }
  }

  /**
   * A benefit line.
   *
   * @param benefitLine HMO, GLI or GPA (list EB_BENEFIT_LINE)
   * @param productCode product, may be empty
   * @param incumbentInsurer insurer party code, may be empty
   * @param currentPolicyNo current policy number
   * @param currentArn current ARN
   * @param periodFrom period start
   * @param periodTo period end
   * @param headcount members covered
   */
  public record LineRequest(
      String benefitLine,
      String productCode,
      String incumbentInsurer,
      String currentPolicyNo,
      String currentArn,
      LocalDate periodFrom,
      LocalDate periodTo,
      Integer headcount) {

    /**
     * The domain data, blanks as null.
     *
     * @return data
     */
    public EbProgrammeLine.Data toData() {
      return new EbProgrammeLine.Data(
          blank(benefitLine),
          blank(productCode),
          blank(incumbentInsurer),
          blank(currentPolicyNo),
          blank(currentArn),
          periodFrom,
          periodTo,
          headcount);
    }
  }

  /**
   * An HR contact.
   *
   * @param name name
   * @param email e-mail
   * @param mobile mobile
   * @param role HR_HEAD, HR_OFFICER or FINANCE
   * @param receivesRa receives the renewal advice
   * @param receivesSoa receives the SOAs
   */
  public record ContactRequest(
      String name,
      String email,
      String mobile,
      EbContactRole role,
      Boolean receivesRa,
      Boolean receivesSoa) {

    /**
     * The domain data.
     *
     * @return data
     */
    public EbProgrammeContact.Data toData() {
      return new EbProgrammeContact.Data(
          name,
          email,
          mobile,
          role,
          Boolean.TRUE.equals(receivesRa),
          Boolean.TRUE.equals(receivesSoa));
    }
  }

  /**
   * Programmes to send the renewal advice to (Send RA).
   *
   * @param programmeIds programmes
   */
  public record SendRaRequest(List<Long> programmeIds) {}

  /**
   * Query parameters of the Programmes work list.
   *
   * @param tab tab, ALL when empty
   * @param stage stage of the open cycle
   * @param ao account officer
   * @param team team
   * @param q programme number, client or programme name
   * @param page page, 0 when empty
   * @param size page size, 20 when empty (at most 200)
   */
  public record ProgrammeFilter(
      ProgrammeQuery.Tab tab,
      String stage,
      String ao,
      String team,
      String q,
      Integer page,
      Integer size) {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 200;

    /**
     * The query criteria.
     *
     * @return criteria
     */
    public ProgrammeQuery.ProgrammeCriteria criteria() {
      return new ProgrammeQuery.ProgrammeCriteria(tab, stage, ao, team, q);
    }

    /**
     * The page asked for.
     *
     * @return zero-based page
     */
    public int pageNo() {
      return page == null ? 0 : Math.max(0, page);
    }

    /**
     * The page size asked for.
     *
     * @return size between 1 and 200
     */
    public int pageSize() {
      return size == null ? DEFAULT_SIZE : Math.clamp(size, 1, MAX_SIZE);
    }
  }

  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
