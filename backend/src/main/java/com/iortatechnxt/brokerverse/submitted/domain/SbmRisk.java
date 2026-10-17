package com.iortatechnxt.brokerverse.submitted.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Risk details of a submitted policy: the vehicle of a motor policy, the property of a fire policy
 * (BRIDSP-04; Report List #151).
 *
 * @param unitDescription unit description
 * @param serialNo serial (chassis) number
 * @param motorNo motor (engine) number
 * @param colour colour
 * @param plateNo plate number
 * @param vehicleType vehicle type (insurer rules)
 * @param vehicleYear year model (vehicle age limits)
 * @param propertyLocation property location
 * @param occupancy occupancy
 * @param mortgagee mortgagee
 */
@Embeddable
public record SbmRisk(
    @Column(name = "unit_description", length = 250) String unitDescription,
    @Column(name = "serial_no", length = 60) String serialNo,
    @Column(name = "motor_no", length = 60) String motorNo,
    @Column(name = "colour", length = 40) String colour,
    @Column(name = "plate_no", length = 20) String plateNo,
    @Column(name = "vehicle_type", length = 40) String vehicleType,
    @Column(name = "vehicle_year") Integer vehicleYear,
    @Column(name = "property_location", length = 500) String propertyLocation,
    @Column(name = "occupancy", length = 120) String occupancy,
    @Column(name = "mortgagee", length = 250) String mortgagee) {

  /** No risk details. */
  public static final SbmRisk NONE =
      new SbmRisk(null, null, null, null, null, null, null, null, null, null);
}
