package com.iortatechnxt.brokerverse.renewal.epolicy.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reading of the e-policy files of an insurer (FRRN.033.01): the summary file (.txt), one record
 * per line with the sequence number, reference number, policy number, PDF file name and document
 * tag separated by a pipe, a tab or a comma (a heading line is skipped), and the ZIP file of the
 * e-policy documents.
 */
public final class EpolicyFiles {

  private static final Pattern SEPARATOR = Pattern.compile("[|\\t,]");
  private static final Pattern DIGITS = Pattern.compile("\\d+");
  private static final int SEQ = 0;
  private static final int REFERENCE = 1;
  private static final int POLICY = 2;
  private static final int PDF = 3;
  private static final int TAG = 4;
  private static final int FIELDS = 5;
  private static final int MAX_FILES = 2000;
  private static final long MAX_BYTES = 200L * 1024 * 1024;

  private EpolicyFiles() {}

  /**
   * The records of a summary file.
   *
   * @param content summary file
   * @return records
   */
  public static List<Summary> summary(byte[] content) {
    List<Summary> out = new ArrayList<>();
    String[] lines = new String(content, StandardCharsets.UTF_8).split("\\r?\\n");
    for (String line : lines) {
      String text = line.strip();
      if (text.isEmpty()) {
        continue;
      }
      String[] f = SEPARATOR.split(text, -1);
      if (!DIGITS.matcher(f[0].strip()).matches()) {
        continue;
      }
      if (f.length < FIELDS) {
        throw new BusinessRuleException(
            "RNW_EPOLICY_SUMMARY",
            "Invalid summary file: line " + f[0].strip() + " must have " + FIELDS + " fields");
      }
      out.add(summary(f));
    }
    if (out.isEmpty()) {
      throw new BusinessRuleException("RNW_EPOLICY_SUMMARY", "The summary file has no record");
    }
    return out;
  }

  private static Summary summary(String... f) {
    return new Summary(
        Integer.parseInt(f[SEQ].strip()),
        f[REFERENCE].strip(),
        f[POLICY].strip(),
        f[PDF].strip(),
        f[TAG].strip());
  }

  /**
   * The documents of a ZIP file by file name (folders ignored).
   *
   * @param zip ZIP file
   * @return documents
   */
  public static Map<String, byte[]> documents(byte[] zip) {
    Map<String, byte[]> out = new LinkedHashMap<>();
    try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
      read(in, out);
    } catch (IOException ex) {
      throw new BusinessRuleException("RNW_EPOLICY_ZIP", "The ZIP file cannot be read", ex);
    }
    if (out.isEmpty()) {
      throw new BusinessRuleException("RNW_EPOLICY_ZIP", "The ZIP file has no document");
    }
    return out;
  }

  private static void read(ZipInputStream in, Map<String, byte[]> out) throws IOException {
    long total = 0;
    for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
      if (e.isDirectory()) {
        continue;
      }
      byte[] bytes = read(in);
      total += bytes.length;
      if (out.size() >= MAX_FILES || total > MAX_BYTES) {
        throw new BusinessRuleException("RNW_EPOLICY_ZIP", "The ZIP file is too large");
      }
      String name = e.getName().replace('\\', '/');
      out.put(name.substring(name.lastIndexOf('/') + 1), bytes);
    }
  }

  private static byte[] read(ZipInputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    in.transferTo(out);
    return out.toByteArray();
  }

  /**
   * A record of the summary file.
   *
   * @param seq sequence number
   * @param reference renewal reference number
   * @param policyNo policy number
   * @param pdfFile PDF file name
   * @param tag document tag
   */
  public record Summary(int seq, String reference, String policyNo, String pdfFile, String tag) {}
}
