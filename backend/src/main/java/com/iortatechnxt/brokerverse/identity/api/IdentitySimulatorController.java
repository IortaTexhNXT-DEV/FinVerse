package com.iortatechnxt.brokerverse.identity.api;

import com.iortatechnxt.brokerverse.identity.api.dto.DirectoryAccountDto;
import com.iortatechnxt.brokerverse.identity.api.dto.IdentityEventResponse;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventType;
import com.iortatechnxt.brokerverse.identity.service.IdentitySimulator;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The accounts of the Enterprise SSO simulator and the events UIDM-ISC would send for them (SIT and
 * UAT only): the System Administrator adds a joiner, changes details or status, and sends the
 * joiner, mover, leaver, rehire or status event to the system.
 */
@RestController
@RequestMapping("/api/v1/admin/identity/simulator")
@PreAuthorize("hasAuthority('USER_MANAGE')")
@ConditionalOnProperty(name = "brokerverse.identity.simulator", havingValue = "true")
public class IdentitySimulatorController {

  private final IdentitySimulator simulator;

  /**
   * Creates the controller.
   *
   * @param simulator the simulator
   */
  public IdentitySimulatorController(IdentitySimulator simulator) {
    this.simulator = simulator;
  }

  /**
   * The accounts of the simulator.
   *
   * @return accounts
   */
  @GetMapping("/accounts")
  public List<DirectoryAccountDto> accounts() {
    return simulator.accounts().stream().map(DirectoryAccountDto::from).toList();
  }

  /**
   * Adds an account.
   *
   * @param account details
   * @return the account
   */
  @PostMapping("/accounts")
  @ResponseStatus(HttpStatus.CREATED)
  public DirectoryAccountDto add(@Valid @RequestBody DirectoryAccountDto account) {
    return DirectoryAccountDto.from(simulator.add(account.toAccount()));
  }

  /**
   * Changes an account.
   *
   * @param windowsId Windows ID
   * @param account details
   * @return the account
   */
  @PutMapping("/accounts")
  public DirectoryAccountDto change(
      @RequestParam String windowsId, @Valid @RequestBody DirectoryAccountDto account) {
    return DirectoryAccountDto.from(simulator.change(windowsId, account.toAccount()));
  }

  /**
   * Sends the event of an account to the system.
   *
   * @param windowsId Windows ID
   * @param type JOINER, MOVER, LEAVER, REHIRE or STATUS
   * @return the event with its outcome
   */
  @PostMapping("/accounts/events/{type}")
  public IdentityEventResponse send(
      @RequestParam String windowsId, @PathVariable IdentityEventType type) {
    return IdentityEventResponse.from(simulator.send(windowsId, type));
  }
}
