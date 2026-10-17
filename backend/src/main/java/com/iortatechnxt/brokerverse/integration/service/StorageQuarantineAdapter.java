package com.iortatechnxt.brokerverse.integration.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.storage.service.FileQuarantineListener;
import org.springframework.stereotype.Component;

/**
 * Tells people about a quarantined file (DOCUMENT_STORAGE_DECISION, decision 2): an in-app
 * notification (event {@code FILE_QUARANTINED}) to the uploader and to every holder of {@code
 * FILE_QUARANTINE_VIEW} (the security role), and the alert {@code FILE_QUARANTINED} on the
 * exception report. Lives in {@code integration} so that {@code storage} depends on neither {@code
 * messaging} nor {@code alert}, whose modules will store their files through it.
 */
@Component
public class StorageQuarantineAdapter implements FileQuarantineListener {

  /** Exception code and notification event. */
  static final String CODE = "FILE_QUARANTINED";

  /** Permission of the security reviewers. */
  static final String REVIEWERS = "FILE_QUARANTINE_VIEW";

  private static final String ENTITY = "StoredFile";

  private final NotificationService notifications;
  private final AlertService alerts;

  /**
   * Creates the adapter.
   *
   * @param notifications in-app notifications
   * @param alerts exception alerts
   */
  public StorageQuarantineAdapter(NotificationService notifications, AlertService alerts) {
    this.notifications = notifications;
    this.alerts = alerts;
  }

  @Override
  public void quarantined(QuarantinedFile file) {
    String id = String.valueOf(file.fileId());
    String message =
        "The file "
            + file.fileName()
            + " of "
            + file.ownerEntityType()
            + " "
            + file.ownerEntityId()
            + " was quarantined by the malware scan ("
            + file.scanResult()
            + ") and cannot be opened";
    Notice notice = new Notice("File quarantined", message, null, ENTITY, id);
    notifications.notifyUser(file.uploadedBy(), notice, CODE);
    notifications.notifyPermission(REVIEWERS, notice, CODE);
    alerts.raise(
        CODE, new AlertFacts(file.companyId(), null, ENTITY, id, message, null, CODE + ":" + id));
  }
}
