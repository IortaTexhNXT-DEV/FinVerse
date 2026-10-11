package com.iortatechnxt.brokerverse.configpromo.engine;

import com.iortatechnxt.brokerverse.common.util.Sha256;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Checksums of dataset content, computed the same way on both sides of a promotion. */
public final class Checksums {

  private Checksums() {}

  /**
   * SHA-256 of rows projected to the compared columns, in the given (natural key) order.
   *
   * @param rows rows
   * @param columns compared columns
   * @return hex checksum
   */
  public static String content(List<Map<String, Object>> rows, List<String> columns) {
    List<Map<String, Object>> projected = new ArrayList<>(rows.size());
    for (Map<String, Object> row : rows) {
      Map<String, Object> p = new TreeMap<>();
      for (String c : columns) {
        p.put(c, row.get(c));
      }
      projected.add(p);
    }
    return Sha256.hex(CanonicalJson.bytes(projected));
  }
}
