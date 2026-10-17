package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * The notices of package dates (BDOI FRS FRPM.004.01): alerts beginning 90 days (setting
 * PACKAGE_ANNIVERSARY_NOTICE_DAYS) before the package anniversary date, once per anniversary, and
 * the recipients of the expiry and anniversary notices: the TSU Officers and TSU Team Leads, and
 * MBS when PACKAGE_EXPIRY_NOTIFY_MBS is on; each in the system and by e-mail as preferred.
 */
@Component
public class PackageDateNotices {

  /** Exception code of the anniversary alert. */
  public static final String ANNIVERSARY_ALERT = "PACKAGE_ANNIVERSARY";

  /** Notification event of the expiry and anniversary notices. */
  public static final String EVENT = "PM_PACKAGE_EXPIRY";

  /** Setting: MBS is told too. */
  public static final String NOTIFY_MBS = "PACKAGE_EXPIRY_NOTIFY_MBS";

  private static final String NOTICE_DAYS = "PACKAGE_ANNIVERSARY_NOTICE_DAYS";
  private static final int DEFAULT_DAYS = 90;
  private static final String TSU = "PKG_NEGOTIATE";
  private static final String MBS = "PRODUCT_MAINTAIN";
  private static final String LINK = "/product-maintenance/expiry";

  private final AlertService alerts;
  private final NoticeDelivery delivery;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the notices.
   *
   * @param alerts exception alerts
   * @param delivery in-app and e-mail delivery
   * @param parameters business parameters
   * @param clock clock
   */
  public PackageDateNotices(
      AlertService alerts,
      NoticeDelivery delivery,
      SystemParameterService parameters,
      Clock clock) {
    this.alerts = alerts;
    this.delivery = delivery;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * Tells the TSU Officers and Team Leads (and MBS by setting).
   *
   * @param notice the notice
   */
  public void tell(Notice notice) {
    delivery.toPermission(TSU, notice, EVENT, true);
    if (Boolean.parseBoolean(parameters.text(NOTIFY_MBS, "false").strip())) {
      delivery.toPermission(MBS, notice, EVENT, true);
    }
  }

  /**
   * Raises the anniversary alerts of a company, once per package anniversary.
   *
   * @param source version reads
   * @param companyId company
   * @return alerts raised
   */
  public int anniversaries(ProductVersionQueryService source, Long companyId) {
    int days = parameters.intValue(NOTICE_DAYS, DEFAULT_DAYS);
    LocalDate today = BusinessClock.today(clock);
    int raised = 0;
    for (ProductVersionView v : source.packagesAtAnniversary(companyId, days)) {
      LocalDate anniversary = v.dates().anniversaryDate();
      String message =
          "Package "
              + v.productCode()
              + " ("
              + v.productName()
              + ") reaches its anniversary on "
              + DisplayFormat.date(anniversary)
              + " ("
              + ChronoUnit.DAYS.between(today, anniversary)
              + " days)";
      boolean alerted =
          alerts
              .raise(
                  ANNIVERSARY_ALERT,
                  new AlertFacts(
                      companyId,
                      null,
                      "Product",
                      v.productCode(),
                      message,
                      null,
                      ANNIVERSARY_ALERT + ":" + v.productCode() + ":" + anniversary))
              .isPresent();
      if (alerted) {
        tell(
            new Notice(
                "Package anniversary: " + v.productCode(),
                message,
                LINK,
                "Product",
                v.productCode()));
        raised++;
      }
    }
    return raised;
  }
}
