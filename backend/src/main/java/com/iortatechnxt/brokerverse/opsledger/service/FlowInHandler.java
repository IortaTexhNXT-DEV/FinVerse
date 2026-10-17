package com.iortatechnxt.brokerverse.opsledger.service;

import java.util.Arrays;

/**
 * Processes the files of one feed (BRQID.004/005). The module owning a feed implements it as a
 * Spring bean; {@link FlowInService#upload} hands every uploaded file of the feed to it (manual
 * upload is the default transport until BDOI gives the interface specifications, OQ01), and a
 * scheduled transport will call the same handler later.
 */
public interface FlowInHandler {

  /**
   * The feed handled ({@code ops_flow_in_feed.code}).
   *
   * @return feed code
   */
  String feedCode();

  /**
   * Reads a file and accepts each of its records through the context.
   *
   * @param file file name and content
   * @param context run context
   */
  void handle(FlowInFile file, FlowInContext context);

  /**
   * A file received for a feed.
   *
   * @param fileName file name
   * @param content bytes
   */
  record FlowInFile(String fileName, byte[] content) {

    /** Defensive copy. */
    public FlowInFile {
      content = content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof FlowInFile f
          && fileName.equals(f.fileName)
          && Arrays.equals(content, f.content);
    }

    @Override
    public int hashCode() {
      return 31 * fileName.hashCode() + Arrays.hashCode(content);
    }

    @Override
    public String toString() {
      return "FlowInFile[" + fileName + ", " + content.length + " bytes]";
    }
  }
}
