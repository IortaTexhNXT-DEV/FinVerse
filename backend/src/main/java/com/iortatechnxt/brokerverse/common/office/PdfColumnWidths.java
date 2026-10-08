package com.iortatechnxt.brokerverse.common.office;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import java.util.Arrays;
import java.util.List;

/**
 * Column widths of a PDF table that break text only between words: every column is at least as wide
 * as the longest word of its heading and, as far as the page allows, as its longest value word (a
 * date or reference stays on one line); the rest of the width is shared in proportion to the column
 * weights. When the words of the headings do not fit the page, the widths follow the weights as
 * before.
 */
public final class PdfColumnWidths {

  /** A value word wider than this share of the table does not widen its column. */
  private static final float LONGEST_VALUE_SHARE = 0.3f;

  private final float[] weights;
  private final float[] headMinimum;
  private final float[] valueMinimum;
  private final float padding;

  /**
   * Starts the widths of a table.
   *
   * @param weights relative widths, one per column
   * @param padding left plus right padding of a cell, in points
   */
  public PdfColumnWidths(float[] weights, float padding) {
    this.weights = weights.clone();
    this.headMinimum = new float[weights.length];
    this.valueMinimum = new float[weights.length];
    this.padding = padding;
  }

  /**
   * Takes the longest word of a heading into account.
   *
   * @param column column index
   * @param heading heading text, may be null
   * @param font heading font
   * @return this
   */
  public PdfColumnWidths heading(int column, String heading, Font font) {
    headMinimum[column] = Math.max(headMinimum[column], longestWord(heading, font));
    return this;
  }

  /**
   * Takes the longest word of a value into account.
   *
   * @param column column index
   * @param value value text, may be null
   * @param font value font
   * @return this
   */
  public PdfColumnWidths value(int column, String value, Font font) {
    valueMinimum[column] = Math.max(valueMinimum[column], longestWord(value, font));
    return this;
  }

  /**
   * Takes the longest words of the rows of a table into account.
   *
   * @param rows rows of value texts
   * @param font value font
   * @return this
   */
  public PdfColumnWidths values(List<List<String>> rows, Font font) {
    for (List<String> row : rows) {
      for (int c = 0; c < Math.min(row.size(), weights.length); c++) {
        value(c, row.get(c), font);
      }
    }
    return this;
  }

  /**
   * The widths for a table of the given total width.
   *
   * @param total table width in points
   * @return one width per column, adding up to the total
   */
  public float[] fit(float total) {
    float cap = total * LONGEST_VALUE_SHARE;
    float[] both = new float[weights.length];
    float[] head = new float[weights.length];
    for (int i = 0; i < weights.length; i++) {
      head[i] = headMinimum[i] > 0 ? headMinimum[i] + padding : 0;
      float value = valueMinimum[i] > 0 ? Math.min(valueMinimum[i], cap) + padding : 0;
      both[i] = Math.max(head[i], value);
    }
    if (sum(both) <= total) {
      return share(both, total);
    }
    if (sum(head) <= total) {
      return share(head, total);
    }
    return share(new float[weights.length], total);
  }

  /**
   * Shares the width in proportion to the weights, raising every column below its minimum to the
   * minimum and taking the difference from the others.
   */
  private float[] share(float[] minimum, float total) {
    float[] width = new float[weights.length];
    boolean[] fixed = new boolean[weights.length];
    boolean changed = true;
    while (changed) {
      changed = false;
      float free = total;
      float freeWeight = 0;
      for (int i = 0; i < width.length; i++) {
        if (fixed[i]) {
          free -= minimum[i];
        } else {
          freeWeight += weights[i];
        }
      }
      for (int i = 0; i < width.length; i++) {
        width[i] = fixed[i] ? minimum[i] : free * weights[i] / Math.max(freeWeight, 1e-6f);
        if (!fixed[i] && width[i] < minimum[i]) {
          fixed[i] = true;
          changed = true;
        }
      }
    }
    return width;
  }

  private static float sum(float[] values) {
    float s = 0;
    for (float v : values) {
      s += v;
    }
    return s;
  }

  /**
   * The width of the longest word of a text (words are separated by spaces).
   *
   * @param text text, may be null
   * @param font font
   * @return width in points, 0 for an empty text
   */
  public static float longestWord(String text, Font font) {
    if (text == null || text.isBlank()) {
      return 0;
    }
    BaseFont base = font.getCalculatedBaseFont(false);
    float size = font.getCalculatedSize();
    return (float)
        Arrays.stream(text.strip().split("\\s+"))
            .mapToDouble(w -> base.getWidthPoint(w, size))
            .max()
            .orElse(0);
  }
}
