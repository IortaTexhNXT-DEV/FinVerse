package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Notification recipients of the submitted policies events. */
public interface SbmNotificationRecipientRepository
    extends JpaRepository<SbmNotificationRecipient, Long> {

  /**
   * Recipients of a company.
   *
   * @param companyId company
   * @return recipients by event
   */
  List<SbmNotificationRecipient> findByCompanyIdOrderByEventCodeAscIdAsc(Long companyId);

  /**
   * Recipients of an event in a status.
   *
   * @param companyId company
   * @param eventCode event
   * @param status ACTIVE
   * @return recipients
   */
  List<SbmNotificationRecipient> findByCompanyIdAndEventCodeAndRecordStatus(
      Long companyId, String eventCode, RecordStatus status);
}
