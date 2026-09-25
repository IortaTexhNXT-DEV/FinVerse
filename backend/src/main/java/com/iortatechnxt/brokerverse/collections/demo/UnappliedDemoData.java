package com.iortatechnxt.brokerverse.collections.demo;

import com.iortatechnxt.brokerverse.collections.unapplied.service.ApplicationFileService;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedDispositionService;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedDispositionService.DispositionInput;
import com.iortatechnxt.brokerverse.opsledger.demo.DemoUsers;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedFilter;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedView;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Collector side of the unapplied-payments demo storyline (wave C1-C, demo profile only,
 * idempotent), after the cashier received the demo payments without a match:
 *
 * <ul>
 *   <li>the Unapplied Payment Handler persona {@code upphandler} (role UNAPPLIED_HANDLER, CQ10) is
 *       created with the demo password;
 *   <li>{@code upphandler} asks Cashiering to apply Grace Villanueva's payment to an open invoice
 *       outside the installment plans of the Collections demo, preferably one without promise or
 *       escalation ("For application to invoice", BRCLXN.030/047) and notes "Coordinate further" on
 *       Juan Dela Cruz's payment;
 *   <li>{@code clxhandler} asks for the refund of Mega Traders Inc.'s payment (BRCLXN.031);
 *   <li>the "For Application To Invoice" file of the day is written (BRCLXN.041).
 * </ul>
 *
 * <p>Liza Manalo's payment is left without a disposition. Cashiering applies the request next
 * ({@code cashiering.demo.CollectorRequestDemoData}). A step that fails is logged and skipped.
 */
@Component
@Profile("demo")
@Order(126)
public class UnappliedDemoData implements ApplicationRunner {

  /** The Unapplied Payment Handler demo user. */
  public static final String UPP_HANDLER = "upphandler";

  private static final Logger LOG = LoggerFactory.getLogger(UnappliedDemoData.class);
  private static final String PASSWORD = "Brokerverse@2026";
  private static final String HANDLER = "clxhandler";
  private static final BigDecimal APPLIED = new BigDecimal("1000.00");
  private static final String TARGET =
      "select i.invoice_no from clx_item i where i.company_id = ? and i.status = 'OPEN'"
          + " and i.net_outstanding >= ?"
          + " and not exists (select 1 from clx_installment_plan p where p.arn = i.arn)"
          + " order by (select count(*) from clx_promise r where r.arn = i.arn)"
          + " + (select count(*) from clx_escalation_item e where e.invoice_no = i.invoice_no),"
          + " i.invoice_no limit 1";

  private final UserAdminService userAdmin;
  private final UnappliedDirectory directory;
  private final UnappliedDispositionService dispositions;
  private final ApplicationFileService files;
  private final CompanyRepository companies;
  private final JdbcTemplate jdbc;
  private final DemoUsers users;

  /**
   * Creates the loader.
   *
   * @param userAdmin user administration
   * @param directory Cashiering's unapplied items
   * @param dispositions collector dispositions
   * @param files application file
   * @param companies companies
   * @param jdbc JDBC (target invoice)
   * @param users demo sign-in
   */
  public UnappliedDemoData(
      UserAdminService userAdmin,
      UnappliedDirectory directory,
      UnappliedDispositionService dispositions,
      ApplicationFileService files,
      CompanyRepository companies,
      JdbcTemplate jdbc,
      DemoUsers users) {
    this.userAdmin = userAdmin;
    this.directory = directory;
    this.dispositions = dispositions;
    this.files = files;
    this.companies = companies;
    this.jdbc = jdbc;
    this.users = users;
  }

  @Override
  public void run(ApplicationArguments args) {
    Optional<Long> company = companies.findByCode("FVI").map(c -> c.getId());
    if (company.isEmpty()) {
      return;
    }
    try {
      createHandler();
      Long companyId = company.get();
      find(companyId, "Grace Villanueva")
          .filter(v -> dispositions.of(companyId, v.unappliedRef()).isEmpty())
          .ifPresent(v -> applyToInvoice(companyId, v));
      find(companyId, "Juan Dela Cruz")
          .filter(v -> dispositions.of(companyId, v.unappliedRef()).isEmpty())
          .ifPresent(v -> dispose(UPP_HANDLER, companyId, v, "COORDINATE_FURTHER", null));
      find(companyId, "Mega Traders Inc.")
          .filter(v -> dispositions.of(companyId, v.unappliedRef()).isEmpty())
          .ifPresent(v -> dispose(HANDLER, companyId, v, "FOR_REFUND", null));
      users.run(UPP_HANDLER, () -> files.publish(companyId, files.previousDay().plusDays(1)));
      LOG.info("Unapplied collector demo data recorded");
    } catch (RuntimeException ex) {
      LOG.warn("Unapplied collector demo data skipped: {}", ex.getMessage());
    }
  }

  private void createHandler() {
    boolean exists =
        users.as(
            "badmin",
            () ->
                userAdmin.listUsers().stream().anyMatch(u -> UPP_HANDLER.equals(u.getUsername())));
    if (!exists) {
      users.run(
          "badmin",
          () ->
              userAdmin.createUser(
                  new UserRequest(
                      UPP_HANDLER,
                      "Paula Unapplied Handler",
                      "upphandler@brokerverse-demo.ph",
                      null,
                      null,
                      Set.of("UNAPPLIED_HANDLER"),
                      true),
                  PASSWORD));
    }
  }

  private Optional<UnappliedView> find(Long companyId, String payor) {
    return directory
        .open(
            companyId,
            new UnappliedFilter(payor, null, null, "UNAPPLIED", null, null),
            PageRequest.of(0, 1))
        .stream()
        .findFirst();
  }

  private void applyToInvoice(Long companyId, UnappliedView v) {
    List<String> target = jdbc.queryForList(TARGET, String.class, companyId, APPLIED);
    if (target.isEmpty()) {
      LOG.info("No open collection account to apply {} to", v.unappliedRef());
    } else {
      dispose(UPP_HANDLER, companyId, v, "FOR_APPLICATION_TO_INVOICE", target.get(0));
    }
  }

  private void dispose(
      String user, Long companyId, UnappliedView v, String code, String invoiceNo) {
    users.run(
        user,
        () ->
            dispositions.dispose(
                companyId,
                v.unappliedRef(),
                new DispositionInput(code, invoiceNo, null, "Demo: " + v.payor())));
  }
}
