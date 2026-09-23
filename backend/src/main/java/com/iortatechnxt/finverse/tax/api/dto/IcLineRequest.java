package com.iortatechnxt.finverse.tax.api.dto;

import com.iortatechnxt.finverse.tax.domain.IcMeasure;
import com.iortatechnxt.finverse.tax.domain.IcSchedule;
import com.iortatechnxt.finverse.tax.domain.NormalBalance;
import com.iortatechnxt.finverse.tax.service.IcLineCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Create / update IC schedule mapping line ({@code schedule} and {@code lineCode} are immutable).
 *
 * @param companyId company
 * @param schedule schedule
 * @param lineCode line code
 * @param description description
 * @param lineOrder print order
 * @param accountFrom range start
 * @param accountTo range end
 * @param reportGroup report group
 * @param normalBalance natural side
 * @param signFactor +1 or -1
 * @param measure balance or movement (optional)
 * @param rbcFactor RBC factor in percent (RBC only)
 */
public record IcLineRequest(
    @NotNull Long companyId,
    @NotNull IcSchedule schedule,
    @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Z0-9_]+") String lineCode,
    @NotBlank @Size(max = 200) String description,
    @Min(0) int lineOrder,
    @Size(max = 30) String accountFrom,
    @Size(max = 30) String accountTo,
    @Size(max = 100) String reportGroup,
    @NotNull NormalBalance normalBalance,
    @Min(-1) @Max(1) int signFactor,
    IcMeasure measure,
    @DecimalMin("0") @DecimalMax("100") BigDecimal rbcFactor) {

  /**
   * Converts to the service command.
   *
   * @return command
   */
  public IcLineCommand toCommand() {
    return new IcLineCommand(
        companyId,
        schedule,
        lineCode,
        description,
        lineOrder,
        accountFrom,
        accountTo,
        reportGroup,
        normalBalance,
        signFactor,
        measure,
        rbcFactor);
  }
}
