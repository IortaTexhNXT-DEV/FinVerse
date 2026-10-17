package com.iortatechnxt.brokerverse.renewal.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.renewal.channel.service.CcmSimulator;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelGateway.Transmission;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelGateways;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The CCM transaction reference shown on the Deliveries table reads as a business reference. */
class CcmSimulatorReferenceTest {

  @Test
  void referenceCarriesNoSimulatorMarker() {
    SystemParameterService parameters = mock(SystemParameterService.class);
    when(parameters.items(ChannelGateways.OUTAGE)).thenReturn(List.of());
    var reply =
        new CcmSimulator(parameters)
            .transmit(
                new Transmission(
                    "CCM-2026-000001",
                    List.of("treasury@client.example"),
                    List.of(),
                    "Renewal advice",
                    "Body",
                    "advice.pdf",
                    new byte[] {1},
                    null));
    assertThat(reply.accepted()).isTrue();
    assertThat(reply.reference()).isEqualTo("TRN-CCM-2026-000001").doesNotContain("SIM");
  }
}
