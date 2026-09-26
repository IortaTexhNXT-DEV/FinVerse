package com.iortatechnxt.brokerverse.brokerclaims.setup.api.dto;

import com.iortatechnxt.brokerverse.brokerclaims.status.domain.ClaimHandler;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.StatusAccess;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.lov.domain.LovType;
import java.time.Instant;
import java.util.function.UnaryOperator;

/** Requests and responses of Claims Setup (FR-CL-040/041/043). */
public final class SetupDtos {

  private SetupDtos() {}

  /**
   * A new status access matrix row.
   *
   * @param statusCode status
   * @param roleCode role
   * @param unitCode unit, blank for any unit
   */
  public record MatrixRowRequest(String statusCode, String roleCode, String unitCode) {}

  /**
   * A row of the status access matrix.
   *
   * @param id row
   * @param statusCode status
   * @param statusLabel status label
   * @param roleCode role
   * @param unitCode unit, null for any unit
   * @param status record status
   * @param maker maker
   * @param authorizedBy checker
   * @param authorizedAt authorization time
   */
  public record MatrixRowResponse(
      Long id,
      String statusCode,
      String statusLabel,
      String roleCode,
      String unitCode,
      RecordStatus status,
      String maker,
      String authorizedBy,
      Instant authorizedAt) {

    /**
     * Maps a row.
     *
     * @param row row
     * @param labels status label lookup
     * @return response
     */
    public static MatrixRowResponse from(StatusAccess row, UnaryOperator<String> labels) {
      return new MatrixRowResponse(
          row.getId(),
          row.getStatusCode(),
          labels.apply(row.getStatusCode()),
          row.getRoleCode(),
          row.getUnitCode(),
          row.getRecordStatus(),
          row.getMaker(),
          row.getAuthorizedBy(),
          row.getAuthorizedAt());
    }
  }

  /**
   * A handler of the register.
   *
   * @param username user
   * @param unitCode unit
   * @param team team
   * @param active whether active
   */
  public record HandlerRequest(String username, String unitCode, String team, boolean active) {}

  /**
   * A handler of the register.
   *
   * @param id row
   * @param username user
   * @param unitCode unit
   * @param team team
   * @param active whether active
   */
  public record HandlerResponse(
      Long id, String username, String unitCode, String team, boolean active) {

    /**
     * Maps a handler.
     *
     * @param h handler
     * @return response
     */
    public static HandlerResponse from(ClaimHandler h) {
      return new HandlerResponse(
          h.getId(), h.getUsername(), h.getUnitCode(), h.getTeam(), h.isActive());
    }
  }

  /**
   * A list maintained on Claims Setup.
   *
   * @param code list code
   * @param name name
   * @param description description
   * @param maintainable whether values may be maintained
   */
  public record ClaimsListResponse(
      String code, String name, String description, boolean maintainable) {

    /**
     * Maps a list type.
     *
     * @param t list type
     * @return response
     */
    public static ClaimsListResponse from(LovType t) {
      return new ClaimsListResponse(
          t.getCode(), t.getName(), t.getDescription(), t.isMaintainable());
    }
  }
}
