package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * The tax details of the clients sent to the insurer with the placement (Ease of Paying Taxes,
 * FR-NB-087): taxpayer name, TIN and registered address, read from the client record when the slip
 * is generated. A missing detail is printed as "Not available" so that the insurer sees it.
 */
@Component
public class ClientTaxDetails {

  /** Text printed for a detail missing on the client record. */
  public static final String NOT_AVAILABLE = "Not available";

  private final ClientRepository clients;

  /**
   * Creates the reader.
   *
   * @param clients client records
   */
  public ClientTaxDetails(ClientRepository clients) {
    this.clients = clients;
  }

  /**
   * The tax details of the client of an account.
   *
   * @param account account
   * @return taxpayer name, TIN and registered address
   */
  public TaxDetails of(Account account) {
    Client client =
        account.getClientId() == null ? null : clients.findById(account.getClientId()).orElse(null);
    if (client == null) {
      return new TaxDetails(text(account.getClientName()), NOT_AVAILABLE, NOT_AVAILABLE);
    }
    String name =
        client.getCorporateName() == null || client.getCorporateName().isBlank()
            ? client.getDisplayName()
            : client.getCorporateName();
    return new TaxDetails(text(name), text(client.getTin()), address(client));
  }

  /**
   * Fingerprint of the tax details of the accounts, to see whether they changed after a slip was
   * generated.
   *
   * @param accounts accounts on a slip
   * @return SHA-256 of the details
   */
  public String fingerprint(List<Account> accounts) {
    String joined =
        accounts.stream()
            .map(a -> a.getArn() + "|" + of(a).joined())
            .collect(Collectors.joining("\n"));
    return PlacementSlipService.sha256(joined.getBytes(StandardCharsets.UTF_8));
  }

  private static String address(Client c) {
    String joined =
        Stream.of(c.getAddressLine(), c.getCity(), c.getProvince(), c.getPostalCode())
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(", "));
    return text(joined);
  }

  private static String text(String value) {
    return value == null || value.isBlank() ? NOT_AVAILABLE : value.trim();
  }

  /**
   * Tax details of a client.
   *
   * @param taxpayerName registered name of the taxpayer
   * @param tin taxpayer identification number
   * @param registeredAddress registered address
   */
  public record TaxDetails(String taxpayerName, String tin, String registeredAddress) {

    /**
     * Whether every detail is on the client record.
     *
     * @return true when nothing is missing
     */
    public boolean complete() {
      return !NOT_AVAILABLE.equals(taxpayerName)
          && !NOT_AVAILABLE.equals(tin)
          && !NOT_AVAILABLE.equals(registeredAddress);
    }

    String joined() {
      return taxpayerName + "|" + tin + "|" + registeredAddress;
    }
  }
}
