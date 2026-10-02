package com.iortatechnxt.brokerverse.common.office;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Properties;

/**
 * The brand of generated files (client requirement 16) from the theme pack of the deployment: the
 * logo, the colours and the font of the client, and the name of the system in texts to users.
 * Reports and business documents in PDF, Word and Excel all take their brand from here, so no
 * client logo or colour is written into the platform.
 *
 * <p>The pack is the folder {@code theme/<pack>} on the classpath with {@code brand.properties} and
 * the logo; it is chosen with the system property {@value #PACK_PROPERTY} or the environment
 * variable {@value #PACK_ENV} ({@value #DEFAULT_PACK} unless set), read once when the class loads.
 */
public final class BrandAssets {

  /** System property naming the theme pack. */
  public static final String PACK_PROPERTY = "brokerverse.theme";

  /** Environment variable naming the theme pack. */
  public static final String PACK_ENV = "BROKERVERSE_THEME";

  /** Theme pack used unless configured. */
  public static final String DEFAULT_PACK = "bdoi";

  private static final Pack PACK = Pack.load(packName());

  /**
   * Name of the system in texts to users (e-mails and their subjects, notifications, messages,
   * report footers, document properties), e.g. "BIBS"; the platform never names itself there.
   */
  public static final String SYSTEM_NAME = PACK.text("system.name");

  /** Table headers and titles. */
  public static final String HEADER = PACK.color("header");

  /** Company line. */
  public static final String PRIMARY = PACK.color("primary");

  /** Group rows and label cells. */
  public static final String BACKGROUND = PACK.color("background");

  /** Rule under the letterhead and above grand totals. */
  public static final String ACCENT = PACK.color("accent");

  /** Subtotal rows and banded rows. */
  public static final String BAND = PACK.color("band");

  /** Every second detail row of a table (banded rows). */
  public static final String ROW_BAND = PACK.color("rowBand");

  /** Grid lines. */
  public static final String GRID = PACK.color("grid");

  /** Word and Excel font. */
  public static final String FONT = PACK.text("font");

  /** Classification printed in the footer of every page. */
  public static final String CONFIDENTIAL = "Confidential";

  /** Logo width / height. */
  public static final float LOGO_RATIO = PACK.ratio();

  /** File name of the logo in generated files. */
  public static final String LOGO_NAME = "logo.png";

  private BrandAssets() {}

  /**
   * The logo of the theme pack (PNG, transparent background).
   *
   * @return PNG bytes
   */
  public static byte[] logoPng() {
    return PACK.logo().clone();
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

  private static String packName() {
    String name = System.getProperty(PACK_PROPERTY);
    if (name == null || name.isBlank()) {
      name = System.getenv(PACK_ENV);
    }
    name = name == null || name.isBlank() ? DEFAULT_PACK : name.strip().toLowerCase(Locale.ROOT);
    if (!name.matches("[a-z0-9-]+")) {
      throw new IllegalStateException("Theme pack name must be a folder name: " + name);
    }
    return name;
  }

  /** The loaded theme pack. */
  private record Pack(String name, Properties values, byte[] logo) {

    static Pack load(String name) {
      String base = "/theme/" + name + "/";
      Properties values = new Properties();
      try (InputStream in = BrandAssets.class.getResourceAsStream(base + "brand.properties")) {
        if (in == null) {
          throw new IllegalStateException("Theme pack " + name + " has no brand.properties");
        }
        values.load(in);
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
      String logoFile = values.getProperty("logo", "logo.png");
      try (InputStream in = BrandAssets.class.getResourceAsStream(base + logoFile)) {
        if (in == null) {
          throw new IllegalStateException("Theme pack " + name + " has no logo " + logoFile);
        }
        return new Pack(name, values, in.readAllBytes());
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }

    String text(String key) {
      String value = values.getProperty(key);
      if (value == null || value.isBlank()) {
        throw new IllegalStateException("Theme pack " + name + " has no " + key);
      }
      return value.strip();
    }

    String color(String key) {
      String value = text("color." + key);
      if (!value.matches("[0-9A-Fa-f]{6}")) {
        throw new IllegalStateException("Colour " + key + " of theme pack " + name + " is not hex");
      }
      return value.toUpperCase(Locale.ROOT);
    }

    float ratio() {
      return Float.parseFloat(text("logo.width")) / Float.parseFloat(text("logo.height"));
    }
  }
}
