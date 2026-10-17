package com.iortatechnxt.brokerverse.party;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.PartyApprovalSource;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** My Approvals names the type of a business partner in words, never by its code. */
class PartyApprovalSourceTest {

  @Test
  @SuppressWarnings("unchecked")
  void partyTypeIsNamedInWords() {
    MasterRecordApprovals records = mock(MasterRecordApprovals.class);
    new PartyApprovalSource(records).pendingFor(mock(ApprovalViewer.class));
    ArgumentCaptor<Function<Party, RecordFacts>> facts = ArgumentCaptor.forClass(Function.class);
    verify(records).pending(any(), eq(Party.class), facts.capture());
    Party party = mock(Party.class);
    when(party.getPartyType()).thenReturn(PartyType.SUPPLIER);
    when(party.getCode()).thenReturn("S-000901");

    RecordFacts row = facts.getValue().apply(party);

    assertThat(row.type()).isEqualTo("Party (supplier)");
  }
}
