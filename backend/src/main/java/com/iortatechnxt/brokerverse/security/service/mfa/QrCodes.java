package com.iortatechnxt.brokerverse.security.service.mfa;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * QR codes of the enrolment of an authenticator app, drawn as SVG (a {@code data:} image the web
 * client shows; no external service ever sees the secret).
 */
public final class QrCodes {

  private static final int QUIET_ZONE = 4;

  private QrCodes() {}

  /**
   * The QR code of a text as an SVG {@code data:} URI.
   *
   * @param text text to encode (an {@code otpauth://} URI)
   * @return data URI
   */
  public static String svgDataUri(String text) {
    BitMatrix matrix;
    try {
      matrix =
          new QRCodeWriter()
              .encode(
                  text,
                  BarcodeFormat.QR_CODE,
                  0,
                  0,
                  Map.of(
                      EncodeHintType.ERROR_CORRECTION,
                      ErrorCorrectionLevel.M,
                      EncodeHintType.MARGIN,
                      QUIET_ZONE,
                      EncodeHintType.CHARACTER_SET,
                      "UTF-8"));
    } catch (WriterException ex) {
      throw new IllegalStateException("The QR code could not be drawn", ex);
    }
    int width = matrix.getWidth();
    int height = matrix.getHeight();
    StringBuilder path = new StringBuilder();
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (matrix.get(x, y)) {
          path.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
        }
      }
    }
    String svg =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 "
            + width
            + " "
            + height
            + "\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\""
            + " fill=\"#fff\"/><path fill=\"#000\" d=\""
            + path
            + "\"/></svg>";
    return "data:image/svg+xml;base64,"
        + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
  }
}
