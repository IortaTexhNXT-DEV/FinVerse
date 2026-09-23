package com.iortatechnxt.finverse.fixedasset.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.fixedasset.domain.DepreciationMethod;
import com.iortatechnxt.finverse.fixedasset.domain.FixedAsset;
import java.math.BigDecimal;

/**
 * Depreciation arithmetic (pure functions).
 *
 * <p>Conventions:
 *
 * <ul>
 *   <li>Full-month convention: a full month is charged for the month of acquisition, none for the
 *       month of disposal. Charges are rounded to centavos (half-even).
 *   <li>Straight line: (cost - residual value) / useful life months.
 *   <li>Declining balance (double-declining): opening net book value x 2 / useful life months.
 *   <li>A charge never takes net book value below the residual value, and the last month of the
 *       useful life charges everything that remains above it (absorbing rounding differences), so
 *       an asset is always fully depreciated at the end of its useful life.
 * </ul>
 */
public final class DepreciationCalculator {

  private static final BigDecimal DOUBLE = BigDecimal.valueOf(2);

  private DepreciationCalculator() {}

  /**
   * Depreciation charge of the next month.
   *
   * @param basis depreciation basis
   * @param position position before the month
   * @return charge (zero when fully depreciated)
   */
  public static BigDecimal monthlyCharge(Basis basis, Position position) {
    BigDecimal depreciable = basis.cost().subtract(basis.residual());
    BigDecimal remaining = depreciable.subtract(position.accumulated());
    if (remaining.signum() <= 0) {
      return Money.zero();
    }
    if (position.months() + 1 >= basis.lifeMonths()) {
      return Money.round(remaining);
    }
    BigDecimal base =
        basis.method() == DepreciationMethod.STRAIGHT_LINE
            ? depreciable
            : basis.cost().subtract(position.accumulated()).multiply(DOUBLE);
    BigDecimal charge =
        base.divide(BigDecimal.valueOf(basis.lifeMonths()), Money.SCALE, Money.ROUNDING);
    return charge.min(remaining);
  }

  /**
   * Position after depreciating a number of months (stops early once fully depreciated).
   *
   * @param basis depreciation basis
   * @param start starting position
   * @param months months to depreciate
   * @return position after the months
   */
  public static Position advance(Basis basis, Position start, int months) {
    BigDecimal accumulated = start.accumulated();
    int charged = start.months();
    for (int i = 0; i < months; i++) {
      BigDecimal charge = monthlyCharge(basis, new Position(accumulated, charged));
      if (charge.signum() == 0) {
        break;
      }
      accumulated = accumulated.add(charge);
      charged++;
    }
    return new Position(Money.round(accumulated), charged);
  }

  /**
   * Charge for a number of months from a position.
   *
   * @param basis depreciation basis
   * @param start starting position
   * @param months months due
   * @return total charge and months actually charged
   */
  public static Charge charge(Basis basis, Position start, int months) {
    Position end = advance(basis, start, months);
    return new Charge(
        end.accumulated().subtract(start.accumulated()), end.months() - start.months());
  }

  /**
   * What is depreciated: cost, residual value, method and useful life.
   *
   * @param cost acquisition cost
   * @param residual residual value
   * @param method method
   * @param lifeMonths useful life in months
   */
  public record Basis(
      BigDecimal cost, BigDecimal residual, DepreciationMethod method, int lifeMonths) {

    /**
     * Basis of an asset.
     *
     * @param asset asset
     * @return basis
     */
    public static Basis of(FixedAsset asset) {
      return new Basis(
          asset.getAcquisitionCost(),
          asset.getResidualValue(),
          asset.getDepreciationMethod(),
          asset.getUsefulLifeMonths());
    }
  }

  /**
   * Accumulated depreciation and number of months charged so far.
   *
   * @param accumulated accumulated depreciation
   * @param months months charged
   */
  public record Position(BigDecimal accumulated, int months) {

    /**
     * Current position of an asset.
     *
     * @param asset asset
     * @return position
     */
    public static Position of(FixedAsset asset) {
      return new Position(asset.getAccumulatedDepreciation(), asset.getMonthsDepreciated());
    }
  }

  /**
   * A depreciation charge.
   *
   * @param amount amount
   * @param months months covered
   */
  public record Charge(BigDecimal amount, int months) {}
}
