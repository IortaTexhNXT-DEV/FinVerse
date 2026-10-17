package com.iortatechnxt.brokerverse.report.render;

import com.iortatechnxt.brokerverse.report.render.ReportChart.ChartKind;
import com.iortatechnxt.brokerverse.report.render.ReportChart.Point;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Draws the chart of a report layout as a PNG image (headless), placed in the PDF and Excel files
 * of the report: a bar or line chart with the value of each group, the axis from zero and the group
 * values as labels under the axis.
 */
public final class ChartImage {

  /** Width of the image in pixels. */
  static final int WIDTH = 1200;

  /** Height of the image in pixels. */
  static final int HEIGHT = 560;

  private static final int LEFT = 140;
  private static final int RIGHT = 40;
  private static final int TOP = 70;
  private static final int BOTTOM = 140;
  private static final int TICKS = 5;
  private static final int MAX_LABEL = 18;
  private static final int TITLE_SIZE = 22;
  private static final int TITLE_BASELINE = 40;
  private static final int TEXT_SIZE = 15;
  private static final int DOT = 10;
  private static final float LINE_WIDTH = 3f;
  private static final int BAR_SHARE = 6;
  private static final int SHARES = 10;
  private static final int MIN_BAR = 4;
  private static final int LABEL_GAP = 24;
  private static final int LABEL_STAGGER = 20;
  private static final int FOOT_GAP = 20;
  private static final double[] NICE_STEPS = {1, 2, 2.5, 5, 10};
  private static final Color SERIES = new Color(0x1F, 0x4E, 0x9A);
  private static final Color GRID = new Color(0xDD, 0xE2, 0xEA);
  private static final Color TEXT = new Color(0x22, 0x2B, 0x38);

  private ChartImage() {}

  /**
   * Adds the chart of a report layout to a PDF, after what is already there.
   *
   * @param doc open PDF document
   * @param chart chart, null for none
   * @param width width in points
   * @param spacingBefore space above the image in points
   */
  public static void addTo(Document doc, ReportChart chart, float width, float spacingBefore) {
    if (chart != null) {
      doc.add(pdfImage(chart, width, spacingBefore));
    }
  }

  /**
   * The chart as an image of a PDF, scaled to a width and centred.
   *
   * @param chart chart
   * @param width width in points
   * @param spacingBefore space above the image in points
   * @return image element
   */
  public static Image pdfImage(ReportChart chart, float width, float spacingBefore) {
    try {
      Image image = Image.getInstance(png(chart));
      image.scaleToFit(width, width * HEIGHT / WIDTH);
      image.setSpacingBefore(spacingBefore);
      image.setAlignment(Element.ALIGN_CENTER);
      return image;
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Draws a chart.
   *
   * @param chart chart
   * @return PNG bytes
   */
  public static byte[] png(ReportChart chart) {
    BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, WIDTH, HEIGHT);
      g.setColor(TEXT);
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, TITLE_SIZE));
      g.drawString(chart.title(), LEFT, TITLE_BASELINE);
      draw(g, chart);
    } finally {
      g.dispose();
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      ImageIO.write(image, "png", out);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return out.toByteArray();
  }

  private static void draw(Graphics2D g, ReportChart chart) {
    List<Point> points = chart.points();
    int plotWidth = WIDTH - LEFT - RIGHT;
    int plotHeight = HEIGHT - TOP - BOTTOM;
    double max =
        points.stream().map(Point::value).mapToDouble(BigDecimal::doubleValue).max().orElse(0);
    double top = niceTop(max);
    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, TEXT_SIZE));
    FontMetrics fm = g.getFontMetrics();
    DecimalFormat amount =
        new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
    for (int i = 0; i <= TICKS; i++) {
      int y = TOP + plotHeight - plotHeight * i / TICKS;
      g.setColor(GRID);
      g.drawLine(LEFT, y, LEFT + plotWidth, y);
      g.setColor(TEXT);
      String tick = amount.format(top * i / TICKS);
      g.drawString(tick, LEFT - 10 - fm.stringWidth(tick), y + fm.getAscent() / 2);
    }
    if (points.isEmpty()) {
      return;
    }
    int slot = plotWidth / points.size();
    int prevX = -1;
    int prevY = -1;
    for (int i = 0; i < points.size(); i++) {
      Point p = points.get(i);
      int x = LEFT + slot * i;
      int h = top <= 0 ? 0 : (int) Math.round(plotHeight * p.value().doubleValue() / top);
      int y = TOP + plotHeight - Math.max(h, 0);
      g.setColor(SERIES);
      if (chart.kind() == ChartKind.LINE) {
        int cx = x + slot / 2;
        g.fillOval(cx - DOT / 2, y - DOT / 2, DOT, DOT);
        if (prevX >= 0) {
          g.setStroke(new BasicStroke(LINE_WIDTH));
          g.drawLine(prevX, prevY, cx, y);
        }
        prevX = cx;
        prevY = y;
      } else {
        int bar = Math.max(slot * BAR_SHARE / SHARES, MIN_BAR);
        g.fillRect(x + (slot - bar) / 2, y, bar, TOP + plotHeight - y);
      }
      g.setColor(TEXT);
      String label = shorten(p.label());
      int lx = x + (slot - fm.stringWidth(label)) / 2;
      g.drawString(
          label, Math.max(lx, x), TOP + plotHeight + LABEL_GAP + (i % 2 == 0 ? 0 : LABEL_STAGGER));
    }
    g.drawString(chart.valueLabel(), LEFT, HEIGHT - FOOT_GAP);
  }

  private static String shorten(String label) {
    String text = label == null ? "" : label;
    return text.length() <= MAX_LABEL ? text : text.substring(0, MAX_LABEL - 1) + "…";
  }

  /** The top of the axis: the next round number above the largest value. */
  static double niceTop(double max) {
    if (max <= 0) {
      return 1;
    }
    double magnitude = Math.pow(10, Math.floor(Math.log10(max)));
    for (double step : NICE_STEPS) {
      if (step * magnitude >= max) {
        return step * magnitude;
      }
    }
    return SHARES * magnitude;
  }
}
