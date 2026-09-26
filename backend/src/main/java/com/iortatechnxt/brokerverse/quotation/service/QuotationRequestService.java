package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationRequest;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationRequest.RequestFacts;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationRequestRepository;
import com.iortatechnxt.brokerverse.quotation.domain.RequestStatus;
import com.iortatechnxt.brokerverse.quotation.service.QuotationProspects.Prospect;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quotation requests (BRNB.041, staging of BRNB.023/028): requests captured from an e-mail (the
 * e-mail is attached to the request as document type REQUEST_EMAIL), uploaded in bulk or fetched
 * from source systems through {@link QuotationRequestSource}. The request inbox lists the requests
 * not yet quoted; a request without a client gets its prospect before it is quoted.
 */
@Service
@Transactional
public class QuotationRequestService {

  /** Entity type of requests (attachments, audit). */
  public static final String ENTITY = "QuotationRequest";

  private final QuotationRequestRepository requests;
  private final QuotationNumbers numbers;
  private final ClientService clients;
  private final QuotationProspects prospects;
  private final ProductCatalogService catalog;
  private final LovService lovs;
  private final List<QuotationRequestSource> sources;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param numbers numbering
   * @param clients clients
   * @param prospects prospect creation
   * @param catalog products
   * @param lovs lists of values
   * @param sources source systems (none until BDOI specifies the HLS interface)
   * @param audit audit trail
   * @param clock clock
   */
  public QuotationRequestService(
      QuotationRequestRepository requests,
      QuotationNumbers numbers,
      ClientService clients,
      QuotationProspects prospects,
      ProductCatalogService catalog,
      LovService lovs,
      List<QuotationRequestSource> sources,
      AuditTrailService audit,
      Clock clock) {
    this.requests = requests;
    this.numbers = numbers;
    this.clients = clients;
    this.prospects = prospects;
    this.catalog = catalog;
    this.lovs = lovs;
    this.sources = List.copyOf(sources);
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Records a received request.
   *
   * @param companyId company
   * @param incoming request
   * @return the request in NEW
   */
  public QuotationRequest receive(Long companyId, IncomingQuotationRequest incoming) {
    LocalDate today = LocalDate.now(clock);
    lovs.requireValid("SOURCE_CHANNEL", incoming.channel(), today);
    lovs.validateOptional("MARKET_SEGMENT", blank(incoming.marketSegment()), today);
    Long clientId = null;
    if (blank(incoming.clientCode()) != null) {
      clientId = clients.requireByCode(companyId, incoming.clientCode().strip()).getId();
    } else if (blank(incoming.prospectName()) == null) {
      throw new BusinessRuleException(
          "QUOTATION_REQUEST_CLIENT", "Give the client code or the prospect's name");
    }
    if (blank(incoming.productCode()) != null) {
      catalog.requireUsableProduct(incoming.productCode().strip());
    }
    if (known(incoming.channel(), incoming.externalRef())) {
      throw new BusinessRuleException(
          "QUOTATION_REQUEST_DUPLICATE",
          "Request "
              + incoming.externalRef()
              + " of "
              + incoming.channel()
              + " was already received");
    }
    QuotationRequest saved =
        requests.save(
            new QuotationRequest(
                companyId,
                numbers.request(),
                new RequestFacts(
                    incoming.channel(),
                    blank(incoming.externalRef()),
                    incoming.receivedAt() == null ? clock.instant() : incoming.receivedAt(),
                    clientId,
                    clientId == null ? blank(incoming.prospectName()) : null,
                    blank(incoming.prospectEmail()),
                    blank(incoming.prospectMobile()),
                    blank(incoming.productCode()),
                    blank(incoming.marketSegment()),
                    blank(incoming.requestedCover()))));
    audit.record(
        ENTITY,
        saved.getRequestNo(),
        AuditAction.CREATE,
        "Quotation request received by " + saved.getChannel());
    return saved;
  }

  /**
   * Whether a source reference was already received.
   *
   * @param channel source channel
   * @param externalRef reference in the source system, may be null
   * @return true when known
   */
  @Transactional(readOnly = true)
  public boolean known(String channel, String externalRef) {
    return blank(externalRef) != null
        && requests.existsByChannelAndExternalRef(channel, externalRef.strip());
  }

  /**
   * Stores the requests of every source system, skipping references already received (BRNB.023).
   *
   * @return number of new requests
   */
  public int pullFromSources() {
    int received = 0;
    for (QuotationRequestSource source : sources) {
      for (IncomingQuotationRequest r : source.fetch()) {
        if (!known(source.channel(), r.externalRef())) {
          receive(source.companyId(), withChannel(r, source.channel()));
          received++;
        }
      }
    }
    return received;
  }

  private static IncomingQuotationRequest withChannel(IncomingQuotationRequest r, String channel) {
    return new IncomingQuotationRequest(
        channel,
        r.externalRef(),
        r.receivedAt(),
        r.clientCode(),
        r.prospectName(),
        r.prospectEmail(),
        r.prospectMobile(),
        r.productCode(),
        r.marketSegment(),
        r.requestedCover());
  }

  /**
   * Creates the prospect named by a request (BRNB.029/063: minimum data) and links it.
   *
   * @param id request
   * @return the request
   */
  public QuotationRequest createProspect(Long id) {
    QuotationRequest request = get(id);
    if (request.getClientId() != null) {
      throw new BusinessRuleException(
          "QUOTATION_REQUEST_HAS_CLIENT", "Request " + request.getRequestNo() + " has a client");
    }
    Client prospect =
        prospects.create(
            request.getCompanyId(),
            new Prospect(
                request.getProspectName(),
                null,
                request.getProspectEmail(),
                request.getProspectMobile(),
                request.getMarketSegment()));
    request.linkClient(prospect.getId());
    audit.record(
        ENTITY, request.getRequestNo(), AuditAction.UPDATE, "Prospect " + prospect.getCode());
    return request;
  }

  /**
   * Closes a request without a quotation.
   *
   * @param id request
   * @param reason reason
   * @return the request
   */
  public QuotationRequest close(Long id, String reason) {
    if (blank(reason) == null) {
      throw new BusinessRuleException("QUOTATION_REQUEST_REASON", "Give the reason for closing");
    }
    QuotationRequest request = get(id);
    request.close(reason.strip());
    audit.record(ENTITY, request.getRequestNo(), AuditAction.UPDATE, "Closed: " + reason.strip());
    return request;
  }

  /**
   * Marks a request quoted (called when its quotation is created).
   *
   * @param id request
   * @param quotation quotation
   */
  public void markQuoted(Long id, Quotation quotation) {
    QuotationRequest request = get(id);
    if (request.getClientId() == null) {
      request.linkClient(quotation.getClientId());
    }
    request.markQuoted(quotation.getId());
    audit.record(
        ENTITY,
        request.getRequestNo(),
        AuditAction.UPDATE,
        "Quoted: " + quotation.getQuotationNo());
  }

  /**
   * One request.
   *
   * @param id id
   * @return request
   */
  @Transactional(readOnly = true)
  public QuotationRequest get(Long id) {
    return requests.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Requests of a company, newest first.
   *
   * @param companyId company
   * @param status status, null for all
   * @param text request number, prospect name or requested cover fragment
   * @param pageable page
   * @return requests
   */
  @Transactional(readOnly = true)
  public Page<QuotationRequest> search(
      Long companyId, RequestStatus status, String text, Pageable pageable) {
    String like = "%" + (text == null ? "" : text.strip().toLowerCase(Locale.ROOT)) + "%";
    Specification<QuotationRequest> spec =
        (root, query, cb) ->
            cb.and(
                cb.equal(root.get("companyId"), companyId),
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status),
                cb.or(
                    cb.like(cb.lower(root.get("requestNo")), like),
                    cb.like(cb.lower(cb.coalesce(root.<String>get("prospectName"), "")), like),
                    cb.like(cb.lower(cb.coalesce(root.<String>get("requestedCover"), "")), like)));
    return requests.findAll(spec, pageable);
  }

  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
