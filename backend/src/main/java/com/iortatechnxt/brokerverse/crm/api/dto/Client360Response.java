package com.iortatechnxt.brokerverse.crm.api.dto;

import com.iortatechnxt.brokerverse.audit.domain.AuditLog;
import com.iortatechnxt.brokerverse.crm.service.Client360;
import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import java.time.Instant;
import java.util.List;

/**
 * Client 360 view (BRNB.099): linked records and flagged gaps.
 *
 * @param records linked records
 * @param warnings missing linkages and data gaps
 */
public record Client360Response(List<ClientRecord> records, List<Warning> warnings) {

  /**
   * Maps the view.
   *
   * @param v view
   * @return response
   */
  public static Client360Response from(Client360 v) {
    return new Client360Response(
        v.records(), v.warnings().stream().map(w -> new Warning(w.code(), w.message())).toList());
  }

  /**
   * A flagged gap.
   *
   * @param code code
   * @param message message
   */
  public record Warning(String code, String message) {}

  /**
   * An audit entry of the client's history.
   *
   * @param id id
   * @param occurredAt time
   * @param username user
   * @param action action
   * @param summary details
   */
  public record HistoryEntry(
      Long id, Instant occurredAt, String username, String action, String summary) {

    /**
     * Maps an audit entry.
     *
     * @param a audit entry
     * @return entry
     */
    public static HistoryEntry from(AuditLog a) {
      return new HistoryEntry(
          a.getId(), a.getOccurredAt(), a.getUsername(), a.getAction().name(), a.getSummary());
    }
  }
}
