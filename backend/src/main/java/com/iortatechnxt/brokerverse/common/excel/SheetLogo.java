package com.iortatechnxt.brokerverse.common.excel;

import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.Units;

/**
 * The logo of the theme pack at the top left of a generated sheet, at a given height and its own
 * proportions: the picture is anchored over as many columns as its width needs, measured as Excel
 * draws the columns (seven pixels a character of the column width), so it is not stretched or
 * squeezed by the column widths. Set the column widths before placing the logo.
 */
public final class SheetLogo {

  private static final int PIXELS_PER_CHAR = 7;
  private static final int MAX_COLUMNS = 50;
  private static final double UNITS_PER_CHAR = 256.0;
  private static final double PIXELS_PER_POINT = 96 / 72.0;

  private SheetLogo() {}

  /**
   * Places the logo in the first row.
   *
   * @param wb workbook
   * @param sheet sheet whose column widths are set
   * @param heightPoints logo height in points
   */
  public static void place(Workbook wb, Sheet sheet, double heightPoints) {
    int picture = wb.addPicture(BrandAssets.logoPng(), Workbook.PICTURE_TYPE_PNG);
    ClientAnchor anchor = wb.getCreationHelper().createClientAnchor();
    double remaining = heightPoints * BrandAssets.LOGO_RATIO * PIXELS_PER_POINT;
    int col = 0;
    while (remaining > columnPixels(sheet, col) && col < MAX_COLUMNS) {
      remaining -= columnPixels(sheet, col);
      col++;
    }
    anchor.setCol1(0);
    anchor.setRow1(0);
    anchor.setCol2(col);
    anchor.setRow2(0);
    anchor.setDx2(Units.pixelToEMU((int) Math.round(remaining)));
    anchor.setDy2(Units.toEMU(heightPoints));
    anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
    sheet.createDrawingPatriarch().createPicture(anchor, picture);
  }

  /**
   * The width of a column in pixels as Excel draws it.
   *
   * @param sheet sheet
   * @param col column index
   * @return pixels
   */
  static double columnPixels(Sheet sheet, int col) {
    double chars = sheet.getColumnWidth(col) / UNITS_PER_CHAR;
    return Math.floor(chars * PIXELS_PER_CHAR);
  }
}
