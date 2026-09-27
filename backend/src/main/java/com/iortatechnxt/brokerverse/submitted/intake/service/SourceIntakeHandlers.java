package com.iortatechnxt.brokerverse.submitted.intake.service;

import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.submitted.intake.service.SourceIntakeHandler.SourceSpec;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The upload handlers of the approved sources of submitted policies (BRIDSP-01; design section 8):
 * LFS insurance report, HLS daily insurance report, CIU report, SPI list, Loan Booking Report and
 * IA masterlist. They share one layout until BDOI supplies the layout of each source (SP SQ01).
 */
@Configuration(proxyBeanMethods = false)
public class SourceIntakeHandlers {

  /**
   * LFS insurance report (CBG Motor).
   *
   * @param masterlist masterlist
   * @param runs intake runs
   * @param lovs lists of values
   * @return handler
   */
  @Bean
  public SourceIntakeHandler sbmLfsInsuranceHandler(
      MasterlistService masterlist, IntakeRunService runs, LovService lovs) {
    return new SourceIntakeHandler(
        new SourceSpec(
            "LFS_INSURANCE", "SBM_LFS_INSURANCE", "LFS insurance report", "CBG_MOTOR", null),
        masterlist,
        runs,
        lovs);
  }

  /**
   * HLS daily insurance report (CBG Fire).
   *
   * @param masterlist masterlist
   * @param runs intake runs
   * @param lovs lists of values
   * @return handler
   */
  @Bean
  public SourceIntakeHandler sbmHlsInsuranceHandler(
      MasterlistService masterlist, IntakeRunService runs, LovService lovs) {
    return new SourceIntakeHandler(
        new SourceSpec(
            "HLS_INSURANCE", "SBM_HLS_INSURANCE", "HLS daily insurance report", "CBG_FIRE", null),
        masterlist,
        runs,
        lovs);
  }

  /**
   * CIU report (CBG Motor).
   *
   * @param masterlist masterlist
   * @param runs intake runs
   * @param lovs lists of values
   * @return handler
   */
  @Bean
  public SourceIntakeHandler sbmCiuHandler(
      MasterlistService masterlist, IntakeRunService runs, LovService lovs) {
    return new SourceIntakeHandler(
        new SourceSpec("CIU", "SBM_CIU", "CIU report", "CBG_MOTOR", null), masterlist, runs, lovs);
  }

  /**
   * SPI consolidated list (segment per row).
   *
   * @param masterlist masterlist
   * @param runs intake runs
   * @param lovs lists of values
   * @return handler
   */
  @Bean
  public SourceIntakeHandler sbmSpiHandler(
      MasterlistService masterlist, IntakeRunService runs, LovService lovs) {
    return new SourceIntakeHandler(
        new SourceSpec("SPI", "SBM_SPI", "SPI consolidated list", null, null),
        masterlist,
        runs,
        lovs);
  }

  /**
   * Loan Booking Report (CBG Motor).
   *
   * @param masterlist masterlist
   * @param runs intake runs
   * @param lovs lists of values
   * @return handler
   */
  @Bean
  public SourceIntakeHandler sbmLoanBookingHandler(
      MasterlistService masterlist, IntakeRunService runs, LovService lovs) {
    return new SourceIntakeHandler(
        new SourceSpec(
            "LOAN_BOOKING", "SBM_LOAN_BOOKING", "Loan Booking Report", "CBG_MOTOR", null),
        masterlist,
        runs,
        lovs);
  }

  /**
   * IA masterlist (CBG Motor).
   *
   * @param masterlist masterlist
   * @param runs intake runs
   * @param lovs lists of values
   * @return handler
   */
  @Bean
  public SourceIntakeHandler sbmIaMasterlistHandler(
      MasterlistService masterlist, IntakeRunService runs, LovService lovs) {
    return new SourceIntakeHandler(
        new SourceSpec("IA_MASTERLIST", "SBM_IA_MASTERLIST", "IA masterlist", "CBG_MOTOR", null),
        masterlist,
        runs,
        lovs);
  }
}
