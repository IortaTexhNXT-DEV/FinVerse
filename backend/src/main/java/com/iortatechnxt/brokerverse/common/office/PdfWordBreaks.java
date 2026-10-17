package com.iortatechnxt.brokerverse.common.office;

import com.lowagie.text.Chunk;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.SplitCharacter;
import com.lowagie.text.pdf.DefaultSplitCharacter;
import com.lowagie.text.pdf.PdfChunk;

/**
 * Line breaks of the text in PDF table cells: between words and after the hyphens of a long
 * reference, never inside a date (09-Oct-2027 stays on one line), as a reader writes them.
 */
public final class PdfWordBreaks implements SplitCharacter {

  /** The line breaks of a cell. */
  public static final PdfWordBreaks INSTANCE = new PdfWordBreaks();

  private static final int DAY = 2;
  private static final int MONTH = 3;
  private static final int YEAR = 4;

  private PdfWordBreaks() {}

  /**
   * A phrase whose lines break as {@link PdfWordBreaks} allows.
   *
   * @param text text, may be null
   * @param font font
   * @return phrase
   */
  // OpenPDF's Phrase extends ArrayList (LooseCoupling false positive).
  @SuppressWarnings("PMD.LooseCoupling")
  public static Phrase phrase(String text, Font font) {
    Chunk chunk = new Chunk(text == null ? "" : text, font);
    chunk.setSplitCharacter(INSTANCE);
    return new Phrase(chunk);
  }

  @Override
  public boolean isSplitCharacter(int start, int current, int end, char[] cc, PdfChunk[] ck) {
    if (cc[current] == '-' && inDate(cc, current)) {
      return false;
    }
    return DefaultSplitCharacter.DEFAULT.isSplitCharacter(start, current, end, cc, ck);
  }

  /**
   * Whether the hyphen at {@code at} is one of the two hyphens of a date written dd-MMM-yyyy.
   *
   * @param cc characters
   * @param at position of the hyphen
   * @return true inside a date
   */
  static boolean inDate(char[] cc, int at) {
    // The first hyphen of a date: two digits before it, then three letters, a hyphen and a year.
    if (matches(cc, at - DAY)) {
      return true;
    }
    // The second hyphen: the day and month before it.
    int dayStart = at - MONTH - 1 - DAY;
    return dayStart >= 0 && matches(cc, dayStart);
  }

  /** Whether a dd-MMM-yyyy date starts at {@code from}. */
  private static boolean matches(char[] cc, int from) {
    int length = DAY + 1 + MONTH + 1 + YEAR;
    if (from < 0 || from + length > cc.length) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      if (!fits(cc[from + i], i)) {
        return false;
      }
    }
    return true;
  }

  /** Whether a character fits position {@code i} of dd-MMM-yyyy. */
  private static boolean fits(char c, int i) {
    if (i == DAY || i == DAY + 1 + MONTH) {
      return c == '-';
    }
    if (i > DAY && i <= DAY + MONTH) {
      return Character.isLetter(c);
    }
    return Character.isDigit(c);
  }
}
