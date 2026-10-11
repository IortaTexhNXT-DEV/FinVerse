package com.iortatechnxt.brokerverse.crm.api;

import com.iortatechnxt.brokerverse.crm.api.dto.ClientSummaryResponse;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Client pickers and summaries used by quotation, proposal and account screens. */
@RestController
@RequestMapping("/api/v1/crm/clients")
@PreAuthorize("hasAuthority('CLIENT_VIEW')")
public class ClientLookupController {

  private final ClientService clients;

  /**
   * Creates the controller.
   *
   * @param clients client service
   */
  public ClientLookupController(ClientService clients) {
    this.clients = clients;
  }

  /**
   * Usable clients whose code or name contains a term.
   *
   * @param companyId company
   * @param q term
   * @return up to 20 clients
   */
  @GetMapping("/lookup")
  public List<ClientSummaryResponse> lookup(
      @RequestParam Long companyId, @RequestParam(defaultValue = "") String q) {
    return clients.lookup(companyId, q).stream().map(ClientSummaryResponse::from).toList();
  }

  /**
   * Summary of one client.
   *
   * @param id client
   * @return summary
   */
  @GetMapping("/{id}/summary")
  public ClientSummaryResponse summary(@PathVariable Long id) {
    return ClientSummaryResponse.from(clients.get(id));
  }
}
