package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.catalog.domain.InsurerBranch;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.PlacementChannel;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Where placements and hold cover requests go (BRNB.071, Q06): the insurer branch placement mailbox
 * from the catalog, else the insurer's placement mailboxes. Only e-mail is built; an insurer set up
 * for SFTP or API is refused ({@code PLACEMENT_CHANNEL_PARKED}).
 */
@Component
public class InsurerDirectory {

  private final InsurerService insurers;

  /**
   * Creates the directory.
   *
   * @param insurers insurer panel
   */
  public InsurerDirectory(InsurerService insurers) {
    this.insurers = insurers;
  }

  /**
   * The usable insurer branch an account is placed with, reachable by e-mail.
   *
   * @param companyId company
   * @param insurerCode insurer party code
   * @param branchCode insurer branch
   * @return insurer, branch and recipients
   */
  public PlacementAddress address(Long companyId, String insurerCode, String branchCode) {
    if (insurerCode == null || branchCode == null) {
      throw new BusinessRuleException(
          "INSURER_NOT_SET", "Choose the insurer and branch on the account before placement");
    }
    InsurerBranch branch = insurers.requireUsableBranch(companyId, insurerCode, branchCode);
    InsurerProfile insurer = insurers.requireUsableInsurer(companyId, insurerCode);
    if (insurer.getPlacementChannel() != PlacementChannel.EMAIL) {
      throw new BusinessRuleException(
          "PLACEMENT_CHANNEL_PARKED",
          insurer.getName()
              + " is set up for "
              + insurer.getPlacementChannel()
              + " placements; only e-mail is available (Q06)");
    }
    List<String> to =
        branch.getPlacementEmail() == null || branch.getPlacementEmail().isBlank()
            ? insurer.getPlacementEmailList().stream().map(String::strip).toList()
            : List.of(branch.getPlacementEmail().strip());
    return new PlacementAddress(insurer.getName(), branch.getName(), to);
  }

  /**
   * Addressing of an insurer branch.
   *
   * @param insurerName insurer name
   * @param branchName branch name
   * @param recipients placement mailboxes
   */
  public record PlacementAddress(String insurerName, String branchName, List<String> recipients) {

    /** Defensive copy. */
    public PlacementAddress {
      recipients = List.copyOf(recipients);
    }
  }
}
