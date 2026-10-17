package com.iortatechnxt.brokerverse.configpromo.service;

import com.iortatechnxt.brokerverse.configpromo.domain.PromotionImport;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import org.springframework.stereotype.Component;

/** Tells the approvers, the preparer and the configured groups about the imports. */
@Component
public class PromotionNotifier {

  private static final String ENTITY = "ConfigImport";
  private static final String LINK = "/admin/config-promotion/imports?import=";

  private final NotificationService notifications;
  private final PromotionSettings settings;

  /**
   * Creates the notifier.
   *
   * @param notifications notifications
   * @param settings settings (groups to tell)
   */
  public PromotionNotifier(NotificationService notifications, PromotionSettings settings) {
    this.notifications = notifications;
    this.settings = settings;
  }

  /**
   * The import waits for approval.
   *
   * @param imp import
   */
  public void submitted(PromotionImport imp) {
    Notice notice =
        notice(
            "Configuration import " + imp.getImportNo() + " to approve",
            "A configuration package was checked and waits for the approval of a second user.",
            imp);
    notifications.notifyPermission("CONFIG_IMPORT_APPROVE", notice, "CONFIG_IMPORT_TO_APPROVE");
  }

  /**
   * The import was applied, rejected or failed.
   *
   * @param imp import
   * @param event notification event
   * @param title title
   * @param body text
   */
  public void decided(PromotionImport imp, String event, String title, String body) {
    Notice notice = notice(title, body, imp);
    notifications.notifyUser(imp.getPreparedBy(), notice, event);
    for (String permission : settings.notifiedPermissions()) {
      notifications.notifyPermission(permission, notice, event);
    }
  }

  private static Notice notice(String title, String body, PromotionImport imp) {
    return new Notice(title, body, LINK + imp.getId(), ENTITY, String.valueOf(imp.getId()));
  }
}
