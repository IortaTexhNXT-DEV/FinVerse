package com.iortatechnxt.brokerverse.opsledger.service.adapter;

import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Default {@link UnappliedDirectory} while no module exposes the Cashiering unapplied items: the
 * collector list is empty (BRCLXN.034-036), so Collections works and is tested on its own.
 */
public class EmptyUnappliedDirectory implements UnappliedDirectory {

  @Override
  public Page<UnappliedView> open(Long companyId, UnappliedFilter filter, Pageable pageable) {
    return Page.empty(pageable);
  }

  @Override
  public Optional<UnappliedView> find(String unappliedRef) {
    return Optional.empty();
  }

  @Override
  public List<UnappliedEvent> history(String unappliedRef) {
    return List.of();
  }
}
