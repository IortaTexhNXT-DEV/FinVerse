package com.iortatechnxt.brokerverse.productmaint.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.catalog.service.IncentiveRules;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestRouting;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Source and the Annex E details of a package request (BDOI FRS FRPM.011.02): checks them,
 * keeps them on the request and reads them back. A request on an insurer's offer needs no quotation
 * slip: the insurer's terms are the first response (no negotiation round).
 */
@Service
@Transactional
public class RequestDetailsService {

  private static final int DEFAULT_MIN_POLICIES = 20;

  private final PackageRequests requests;
  private final ObjectMapper json;
  private final LovService lov;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests package requests
   * @param json JSON of the details
   * @param lov lists of values (business origin)
   * @param parameters business parameters (least policies and premium)
   * @param clock clock
   */
  public RequestDetailsService(
      PackageRequests requests,
      ObjectMapper json,
      LovService lov,
      SystemParameterService parameters,
      Clock clock) {
    this.requests = requests;
    this.json = json;
    this.lov = lov;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * Checks the details before a request is saved.
   *
   * @param source source (MARKETING, TSU, INSURER), null for Marketing
   * @param d details, may be null
   */
  @Transactional(readOnly = true)
  public void check(String source, RequestDetails d) {
    new RequestRouting().describe(source, null);
    if (d == null) {
      return;
    }
    checkVolume(d);
    if (d.businessOrigin() != null && !d.businessOrigin().isBlank()) {
      lov.requireValid("PKG_BUSINESS_ORIGIN", d.businessOrigin(), BusinessClock.today(clock));
    }
    IncentiveRules.check(d.incentiveEligible(), d.incentiveAmount(), d.incentiveRate());
  }

  private void checkVolume(RequestDetails d) {
    int policies = parameters.intValue("PKG_MIN_POLICIES", DEFAULT_MIN_POLICIES);
    if (d.estimatedPolicies() != null && d.estimatedPolicies() < policies) {
      throw new BusinessRuleException(
          "PKG_MIN_POLICIES",
          "The estimated number of policies to be issued must be at least " + policies);
    }
    BigDecimal premium = new BigDecimal(parameters.text("PKG_MIN_PREMIUM", "5000000").strip());
    if (d.estimatedPremium() != null && d.estimatedPremium().compareTo(premium) < 0) {
      throw new BusinessRuleException(
          "PKG_MIN_PREMIUM",
          "The estimated total basic premium must be at least " + DisplayFormat.amount(premium));
    }
  }

  /**
   * Keeps the source and the details on a request.
   *
   * @param id request
   * @param source source
   * @param d details, may be null
   * @return the request
   */
  public PackageRequest describe(Long id, String source, RequestDetails d) {
    check(source, d);
    PackageRequest p = requests.get(id);
    try {
      p.getRouting().describe(source, d == null ? null : json.writeValueAsString(d));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Request details cannot be written", e);
    }
    if (RequestRouting.INSURER.equals(p.getRouting().getSource())) {
      p.withoutNegotiation();
    }
    return p;
  }

  /**
   * The details of a request.
   *
   * @param p request
   * @return details, null when none
   */
  @Transactional(readOnly = true)
  public RequestDetails details(PackageRequest p) {
    String text = p.getRouting().getDetails();
    if (text == null || text.isBlank()) {
      return null;
    }
    try {
      return json.readValue(text, RequestDetails.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Request details cannot be read", e);
    }
  }
}
