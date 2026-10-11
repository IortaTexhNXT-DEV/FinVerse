package com.iortatechnxt.brokerverse.common.office;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Column widths of a PDF table that break text only where a reader expects it: every column is at
 * least as wide as the longest word of its heading and as the longest part of its values that has
 * no space or hyphen (an amount such as 14,601.14 or a code such as MTR10 is never cut; a date or a
 * reference may break after a hyphen). As far as the page allows, whole value words are kept on one
 * line too (a date before a long reference). The rest of the width is shared in proportion to the
 * column weights. When even the headings do not fit the page, the widths follow the weights.
 */
public final class PdfColumnWidths {

  /** A value word wider than this share of the table does not widen its column. */
  private static final float LONGEST_VALUE_SHARE = 0.3f;

  /** Room left beside a word that just fits (borders and rounding of the line layout). */
  private static final float SLACK = 3f;

  /** A date as printed, with an optional trailing punctuation mark. */
  private static final java.util.regex.Pattern DATE =
      java.util.regex.Pattern.compile("\\d{2}-[A-Za-z]{3}-\\d{4}\\p{Punct}?");

  /** Guards the division when every column is fixed at its minimum. */
  private static final float MIN_WEIGHT = 1e-6f;

  private final float[] weights;
  private final float[] headMinimum;
  private final float[] partMinimum;
  private final float[] wordMinimum;
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
    this.partMinimum = new float[weights.length];
    this.wordMinimum = new float[weights.length];
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
    wordMinimum[column] = Math.max(wordMinimum[column], longestWord(value, font));
    partMinimum[column] = Math.max(partMinimum[column], longestPart(value, font));
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
   * Whether every heading word and every unbreakable value part fits a table of this width.
   *
   * @param total table width in points
   * @return true when no word has to be cut
   */
  public boolean fits(float total) {
    float cap = total * LONGEST_VALUE_SHARE;
    float sum = 0;
    for (int i = 0; i < weights.length; i++) {
      sum += Math.max(room(headMinimum[i], cap), room(partMinimum[i], cap));
    }
    return sum <= total;
  }

  /**
   * The widths for a table of the given total width.
   *
   * @param total table width in points
   * @return one width per column, adding up to the total
   */
  public float[] fit(float total) {
    float cap = total * LONGEST_VALUE_SHARE;
    float[] minimum = new float[weights.length];
    float[] whole = new float[weights.length];
    for (int i = 0; i < weights.length; i++) {
      minimum[i] = Math.max(room(headMinimum[i], cap), room(partMinimum[i], cap));
      whole[i] = Math.max(minimum[i], room(wordMinimum[i], cap));
    }
    if (sum(minimum) > total) {
      return share(new float[weights.length], total);
    }
    // Whole value words are kept column by column, the columns needing the least extra width
    // first (a date before a long reference, which may still break after its hyphens).
    Integer[] order = new Integer[weights.length];
    for (int i = 0; i < order.length; i++) {
      order[i] = i;
    }
    Arrays.sort(order, Comparator.comparingDouble(i -> whole[i] - minimum[i]));
    float used = sum(minimum);
    for (int i : order) {
      float extra = whole[i] - minimum[i];
      if (extra > 0 && used + extra <= total) {
        minimum[i] = whole[i];
        used += extra;
      }
    }
    return share(minimum, total);
  }

  /** The width a text of this width needs in a cell, at most the cap; 0 for no text. */
  private float room(float text, float cap) {
    return text > 0 ? Math.min(text, cap) + padding + SLACK : 0;
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
      float[] free = free(minimum, fixed, total);
      for (int i = 0; i < width.length; i++) {
        width[i] = fixed[i] ? minimum[i] : free[0] * weights[i] / Math.max(free[1], MIN_WEIGHT);
        if (!fixed[i] && width[i] < minimum[i]) {
          fixed[i] = true;
          changed = true;
        }
      }
    }
    return width;
  }

  /** The width and the weight of the columns not fixed at their minimum. */
  private float[] free(float[] minimum, boolean[] fixed, float total) {
    float free = total;
    float freeWeight = 0;
    for (int i = 0; i < weights.length; i++) {
      if (fixed[i]) {
        free -= minimum[i];
      } else {
        freeWeight += weights[i];
      }
    }
    return new float[] {free, freeWeight};
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
    return longest(text, font, "\\s+");
  }

  /**
   * The width of the longest part of a text that a line may not break: words, and the parts of a
   * hyphenated word up to and with their hyphen.
   *
   * @param text text, may be null
   * @param font font
   * @return width in points, 0 for an empty text
   */
  public static float longestPart(String text, Font font) {
    if (text == null || text.isBlank()) {
      return 0;
    }
    float longest = 0;
    for (String word : text.strip().split("\\s+")) {
      // A date (09-Oct-2026) is never broken at its hyphens (PdfWordBreaks).
      longest =
          Math.max(
              longest,
              DATE.matcher(word).matches()
                  ? longestWord(word, font)
                  : longest(word, font, "(?<=-)"));
    }
    return longest;
  }

  private static float longest(String text, Font font, String separators) {
    if (text == null || text.isBlank()) {
      return 0;
    }
    BaseFont base = font.getCalculatedBaseFont(false);
    float size = font.getCalculatedSize();
    return (float)
        Arrays.stream(text.strip().split(separators))
            .mapToDouble(w -> base.getWidthPoint(w, size))
            .max()
            .orElse(0);
  }
}
