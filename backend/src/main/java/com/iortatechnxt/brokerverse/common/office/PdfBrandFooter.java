package com.iortatechnxt.brokerverse.common.office;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;

/**
 * The branded page footer of generated PDFs: the classification and footer text on the left
 * (shortened with an ellipsis when it would reach the page number) and "prefix Page n of m" on the
 * right, on every page. Register it once the document is open ("of m" is a template filled when the
 * document closes).
 */
public final class PdfBrandFooter extends PdfPageEventHelper {

  private static final float FOOTER_Y = 16f;
  private static final float TEMPLATE_WIDTH = 30f;
  private static final float TEMPLATE_HEIGHT = 12f;
  private static final float FONT_SIZE = 7f;
  private static final float GAP = 12f;
  private static final String ELLIPSIS = "...";
  private static final String SEPARATOR = "  |  ";

  private final String left;
  private final String prefix;
  private final PdfTemplate total;
  private final BaseFont font;

  /**
   * Creates the footer.
   *
   * @param footerText text after the classification, blank for none
   * @param prefix text before "Page n of m", blank for none
   * @param writer writer of the open document
   */
  public PdfBrandFooter(String footerText, String prefix, PdfWriter writer) {
    this.left = classified(footerText);
    this.prefix = prefix == null || prefix.isBlank() ? "" : prefix + SEPARATOR;
    this.total = writer.getDirectContent().createTemplate(TEMPLATE_WIDTH, TEMPLATE_HEIGHT);
    this.font = new Font(Font.HELVETICA).getCalculatedBaseFont(false);
  }

  /**
   * The footer text with the "Confidential" classification in front, unless it already says so.
   *
   * @param footerText text, may be blank
   * @return text to print
   */
  public static String classified(String footerText) {
    String text = footerText == null ? "" : footerText.strip();
    if (text.isEmpty()) {
      return BrandAssets.CONFIDENTIAL;
    }
    if (text.toLowerCase(Locale.ROOT).contains(BrandAssets.CONFIDENTIAL.toLowerCase(Locale.ROOT))) {
      return text;
    }
    return BrandAssets.CONFIDENTIAL + SEPARATOR + text;
  }

  /**
   * The BDO Insure logo scaled to a height.
   *
   * @param height height in points
   * @return image
   */
  public static Image logo(float height) {
    try {
      Image image = Image.getInstance(BrandAssets.logoPng());
      image.scaleAbsolute(height * BrandAssets.LOGO_RATIO, height);
      return image;
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public void onEndPage(PdfWriter writer, Document document) {
    PdfContentByte cb = writer.getDirectContent();
    String text = prefix + "Page " + writer.getPageNumber() + " of ";
    float x = document.right() - font.getWidthPoint(text, FONT_SIZE) - TEMPLATE_WIDTH;
    cb.beginText();
    cb.setFontAndSize(font, FONT_SIZE);
    cb.setColorFill(Color.GRAY);
    cb.setTextMatrix(x, FOOTER_Y);
    cb.showText(text);
    String fitted = fit(left, x - GAP - document.left());
    if (!fitted.isEmpty()) {
      cb.setTextMatrix(document.left(), FOOTER_Y);
      cb.showText(fitted);
    }
    cb.endText();
    cb.addTemplate(total, x + font.getWidthPoint(text, FONT_SIZE), FOOTER_Y);
  }

  /** The text, shortened with an ellipsis to the available width (empty when nothing fits). */
  private String fit(String text, float width) {
    if (font.getWidthPoint(text, FONT_SIZE) <= width) {
      return text;
    }
    String shortened = text;
    while (!shortened.isEmpty() && font.getWidthPoint(shortened + ELLIPSIS, FONT_SIZE) > width) {
      shortened = shortened.substring(0, shortened.length() - 1);
    }
    return shortened.isEmpty() ? "" : shortened.strip() + ELLIPSIS;
  }

  @Override
  public void onCloseDocument(PdfWriter writer, Document document) {
    total.beginText();
    total.setFontAndSize(font, FONT_SIZE);
    total.setColorFill(Color.GRAY);
    total.setTextMatrix(0, 0);
    total.showText(String.valueOf(writer.getPageNumber()));
    total.endText();
  }
}
