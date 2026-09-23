package com.iortatechnxt.finverse.claims.demo;

import java.util.Map;

/**
 * Typical loss narrative of each demo line of business.
 *
 * @param nature nature of loss
 * @param cause cause of loss
 * @param location place of loss
 * @param description narrative
 */
record DemoLoss(String nature, String cause, String location, String description) {

  private static final DemoLoss GENERAL =
      new DemoLoss(
          "Accidental damage", "Accident", "Metro Manila", "Accidental damage to insured property");

  private static final Map<String, DemoLoss> BY_LINE =
      Map.of(
          "FIRE",
          new DemoLoss(
              "Fire",
              "Electrical short circuit",
              "Calamba, Laguna",
              "Fire in the warehouse damaged stocks and part of the building"),
          "MOTOR",
          new DemoLoss(
              "Collision",
              "Rear-end collision at an intersection",
              "EDSA, Quezon City",
              "Insured vehicle hit by a third-party vehicle; body and bumper damage"),
          "MARINE",
          new DemoLoss(
              "Cargo damage",
              "Sea water ingress during typhoon",
              "Port of Manila",
              "Part of the shipment arrived wet and unusable"),
          "ENGG",
          new DemoLoss(
              "Collapse",
              "Formwork failure",
              "Makati City",
              "Partial collapse of scaffolding and formwork at the project site"),
          "CASUALTY",
          new DemoLoss(
              "Third-party injury",
              "Slip and fall on premises",
              "Makati City",
              "Customer injured on the insured's premises; liability claim"),
          "PA",
          new DemoLoss(
              "Accidental injury",
              "Road accident",
              "Cebu City",
              "Insured injured in a road accident; hospitalisation and disability"),
          "HEALTH",
          new DemoLoss(
              "Hospitalisation",
              "Dengue fever",
              "Davao City",
              "Member confined for dengue fever; hospital bill"),
          "BONDS",
          new DemoLoss(
              "Contract default",
              "Contractor failed to complete works",
              "Pasig City",
              "Obligee called the performance bond after contractor default"));

  /**
   * Narrative of a line of business.
   *
   * @param businessLine line of business
   * @return loss narrative (a general one for unknown lines)
   */
  static DemoLoss of(String businessLine) {
    return BY_LINE.getOrDefault(businessLine, GENERAL);
  }
}
