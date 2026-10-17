package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.csf.domain.CsfContactChange;
import com.iortatechnxt.brokerverse.csf.domain.CsfVerification;
import com.iortatechnxt.brokerverse.csf.domain.CsfVerificationRepository;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.ChangeView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.FieldView;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contact changes as shown (Contact History tab, Contact Changes list): the field rows, the result
 * of the verification used and the state of the Operations hand-off of a referral.
 */
@Component
@Transactional(readOnly = true)
public class ChangeViews {

  private final CsfVerificationRepository verifications;
  private final HandoffService handoffs;

  /**
   * Creates the mapper.
   *
   * @param verifications verifications
   * @param handoffs Operations hand-offs
   */
  public ChangeViews(CsfVerificationRepository verifications, HandoffService handoffs) {
    this.verifications = verifications;
    this.handoffs = handoffs;
  }

  /**
   * Views of changes.
   *
   * @param changes changes with their field rows
   * @return views in the same order
   */
  public List<ChangeView> of(List<CsfContactChange> changes) {
    Map<Long, CsfVerification> used =
        verifications
            .findAllById(
                changes.stream()
                    .map(CsfContactChange::getVerificationId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList())
            .stream()
            .collect(Collectors.toMap(CsfVerification::getId, Function.identity()));
    return changes.stream().map(c -> view(c, used.get(c.getVerificationId()))).toList();
  }

  private ChangeView view(CsfContactChange c, CsfVerification v) {
    String handoff =
        c.getHandoffId() == null
            ? null
            : handoffs
                .find(CsfCodes.PORT_FULFILMENT, CsfCodes.MODULE, c.getChangeNo())
                .map(OpsHandoff::getStatus)
                .map(Enum::name)
                .orElse(null);
    return new ChangeView(
        c.getId(),
        c.getChangeNo(),
        c.getClientId(),
        c.getClientCode(),
        c.getClientName(),
        c.getStatus().name(),
        c.getAppliedAt(),
        c.getAgent(),
        c.getChannel(),
        c.getReasonCode(),
        c.getRemarks(),
        v == null ? null : v.getResult().name(),
        v == null ? null : v.getMatches(),
        c.getSyncStatus().name(),
        handoff,
        c.getFields().stream()
            .map(f -> new FieldView(f.getField(), f.getOldValue(), f.getNewValue()))
            .toList());
  }
}
