package com.iortatechnxt.brokerverse.report.core;

import java.util.List;

/** Several {@link CodeSetSource}s declared by one module in one bean. */
public interface CodeSetSourceGroup {

  /**
   * The sources of the group.
   *
   * @return sources
   */
  List<CodeSetSource> sources();
}
