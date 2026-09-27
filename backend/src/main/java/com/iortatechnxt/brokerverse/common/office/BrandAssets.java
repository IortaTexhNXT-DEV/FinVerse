package com.iortatechnxt.brokerverse.common.office;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * The BDO Insure brand of generated files (BDO_UX_GUIDELINES; client requirement 16): the logo, the
 * colours of the style guide and the fonts. Reports and business documents in PDF, Word and Excel
 * all take their brand from here.
 */
public final class BrandAssets {

  /** Header Blue, table headers and titles. */
  public static final String HEADER_BLUE = "004EA8";

  /** CTA Blue, company line. */
  public static final String CTA_BLUE = "0072D8";

  /** Background Blue, group rows and label cells. */
  public static final String BACKGROUND_BLUE = "E5F5FF";

  /** Gold rule under the letterhead and above grand totals. */
  public static final String GOLD = "FDB913";

  /** Light grey of subtotal rows and banded rows. */
  public static final String BAND = "F4F6FA";

  /** Every second detail row of a table (banded rows). */
  public static final String ROW_BAND = "F7FAFD";

  /** Grid lines. */
  public static final String GRID = "D5DBE5";

  /** Word and Excel font: Nunito is the brand font, Arial the approved fallback for files. */
  public static final String FONT = "Arial";

  /** Classification printed in the footer of every page. */
  public static final String CONFIDENTIAL = "Confidential";

  /** Logo width / height in pixels (378 x 77). */
  public static final float LOGO_RATIO = 378f / 77f;

  private static final String LOGO = "/brand/bdo-insure.png";

  private BrandAssets() {}

  /**
   * The BDO Insure logo (PNG, transparent background).
   *
   * @return PNG bytes
   */
  public static byte[] logoPng() {
    return Logo.BYTES.clone();
  }

  /**
   * A brand colour for the PDF renderers.
   *
   * @param hex six hex digits
   * @return colour
   */
  public static Color color(String hex) {
    return Color.decode("#" + hex);
  }

  /** Loads the logo once. */
  private static final class Logo {
    private static final byte[] BYTES = load();

    private static byte[] load() {
      try (InputStream in = BrandAssets.class.getResourceAsStream(LOGO)) {
        if (in == null) {
          throw new IllegalStateException("Brand logo " + LOGO + " is missing");
        }
        return in.readAllBytes();
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }
  }
}
