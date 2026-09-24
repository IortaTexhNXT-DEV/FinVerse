package com.iortatechnxt.brokerverse.underwriting.demo;

import com.iortatechnxt.brokerverse.underwriting.api.dto.ProductRequest;
import com.iortatechnxt.brokerverse.underwriting.domain.UprBasis;
import java.math.BigDecimal;
import java.util.List;

/**
 * Static reference values of the underwriting demo data: products with Philippine tax settings,
 * risk profiles per line of business, clients and locations. Values are deterministic so every demo
 * start produces the same portfolio.
 */
final class DemoCatalog {

  /** Demo clients (policyholders) created by the demo migrations. */
  static final List<String> CLIENTS =
      List.of("C-000101", "C-000102", "C-000201", "C-000202", "C-000203", "C-000204");

  /** Corporate names used as insured for corporate clients. */
  static final List<String> INSURED =
      List.of(
          "Juan Dela Cruz",
          "Maria Clara Santos",
          "Luzon Steel Manufacturing Corp.",
          "Visayas Shipping Lines Inc.",
          "Mindanao Agri Ventures Inc.",
          "Metro Retail Holdings Corp.");

  /** Accumulation zones (catastrophe exposure). */
  static final List<String> ZONES =
      List.of("NCR-MAKATI", "NCR-QUEZON", "LAGUNA-CALAMBA", "CEBU-MANDAUE", "DAVAO-CITY");

  /** Occupations of property risks. */
  static final List<String> OCCUPATIONS =
      List.of(
          "Steel mill",
          "Warehouse - general merchandise",
          "Shopping mall",
          "Office building",
          "Agri processing plant",
          "Cold storage");

  /** Branch codes. */
  static final List<String> BRANCHES = List.of("HO", "CEB", "DVO");

  /** Product profiles in rotation order. */
  static final List<ProductProfile> PRODUCTS =
      List.of(
          new ProductProfile(
              "FIRE-COM",
              "Fire - Commercial",
              "FIRE",
              "20",
              "2",
              5_000_000,
              150_000_000,
              15,
              35,
              "Building, machinery and stocks"),
          new ProductProfile(
              "MOTOR-PC",
              "Motor - Private Car Comprehensive",
              "MOTOR",
              "15",
              "0",
              800_000,
              3_500_000,
              250,
              350,
              "Private car, comprehensive cover"),
          new ProductProfile(
              "MARINE-CGO",
              "Marine Cargo",
              "MARINE",
              "17.5",
              "0",
              2_000_000,
              40_000_000,
              20,
              50,
              "Imported cargo, all risks"),
          new ProductProfile(
              "ENGG-CAR",
              "Engineering - Contractors All Risks",
              "ENGG",
              "15",
              "0",
              20_000_000,
              300_000_000,
              10,
              25,
              "Construction works and plant"),
          new ProductProfile(
              "CAS-CGL",
              "Casualty - General Liability",
              "CASUALTY",
              "15",
              "0",
              5_000_000,
              50_000_000,
              30,
              80,
              "Public and products liability"),
          new ProductProfile(
              "PA-IND",
              "Personal Accident - Individual",
              "PA",
              "20",
              "0",
              500_000,
              5_000_000,
              30,
              60,
              "Accidental death and disablement"),
          new ProductProfile(
              "HEALTH-GRP",
              "Group Health",
              "HEALTH",
              "10",
              "0",
              1_000_000,
              20_000_000,
              300,
              600,
              "Group hospitalization"),
          new ProductProfile(
              "BONDS-SUR",
              "Surety Bonds",
              "BONDS",
              "15",
              "0",
              1_000_000,
              30_000_000,
              50,
              150,
              "Performance and bid bonds"));

  private static final long PRIME_A = 7919L;
  private static final long PRIME_B = 104_729L;
  private static final long THOUSAND = 1000L;

  private DemoCatalog() {}

  /**
   * Product master request of a profile (health is VAT exempt and pays premium tax instead).
   *
   * @param companyId company
   * @param p profile
   * @return request
   */
  static ProductRequest productRequest(Long companyId, ProductProfile p) {
    boolean health = "HEALTH".equals(p.lob());
    return new ProductRequest(
        companyId,
        p.code(),
        p.name(),
        p.lob(),
        new BigDecimal(p.commission()),
        UprBasis.DAYS_365,
        new BigDecimal("12.5"),
        health ? BigDecimal.ZERO : new BigDecimal("12"),
        new BigDecimal("0.75"),
        new BigDecimal(p.fst()),
        health ? BigDecimal.TWO : BigDecimal.ZERO,
        "PA-IND".equals(p.code()) ? new BigDecimal("150") : new BigDecimal("250"),
        "MARINE".equals(p.lob()));
  }

  /**
   * Deterministic pseudo-random fraction in thousandths for a policy index and a salt.
   *
   * @param index policy index
   * @param salt value distinguishing different draws
   * @return value in [0, 999]
   */
  static int draw(int index, int salt) {
    return (int) (((index + 1L) * PRIME_A + salt * PRIME_B) % THOUSAND);
  }

  /**
   * Risk profile of a demo product.
   *
   * @param code product code
   * @param name product name
   * @param lob line of business
   * @param commission default commission %
   * @param fst fire service tax %
   * @param minSumInsured smallest sum insured
   * @param maxSumInsured largest sum insured
   * @param minRateBp lowest premium rate in basis points
   * @param maxRateBp highest premium rate in basis points
   * @param description risk description
   */
  record ProductProfile(
      String code,
      String name,
      String lob,
      String commission,
      String fst,
      long minSumInsured,
      long maxSumInsured,
      int minRateBp,
      int maxRateBp,
      String description) {}
}
