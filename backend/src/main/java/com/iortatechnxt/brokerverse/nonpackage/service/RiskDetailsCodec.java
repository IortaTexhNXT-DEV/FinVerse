package com.iortatechnxt.brokerverse.nonpackage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.RiskDetails;
import org.springframework.stereotype.Component;

/** Stores the PRF risk details as JSON (sections and items). */
@Component
public class RiskDetailsCodec {

  private final ObjectMapper mapper;

  /**
   * Creates the codec.
   *
   * @param mapper JSON mapper
   */
  public RiskDetailsCodec(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * Details as JSON.
   *
   * @param details details
   * @return JSON
   */
  public String toJson(RiskDetails details) {
    try {
      return mapper.writeValueAsString(details);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("PRF risk details cannot be written", e);
    }
  }

  /**
   * The details of a PRF.
   *
   * @param proposal PRF
   * @return details
   */
  public RiskDetails of(ProposalRequest proposal) {
    try {
      return proposal.getRiskDetails() == null
          ? RiskDetails.EMPTY
          : mapper.readValue(proposal.getRiskDetails(), RiskDetails.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("PRF risk details cannot be read", e);
    }
  }
}
