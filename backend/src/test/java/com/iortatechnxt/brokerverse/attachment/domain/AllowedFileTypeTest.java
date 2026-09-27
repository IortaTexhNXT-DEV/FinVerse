package com.iortatechnxt.brokerverse.attachment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** The seven file types of the Customer Servicing Facility uploads (BRCSF-007). */
class AllowedFileTypeTest {

  private static byte[] ascii(String text) {
    return text.getBytes(StandardCharsets.US_ASCII);
  }

  private static byte[] iso(String brand) {
    byte[] head = new byte[16];
    System.arraycopy(ascii("ftyp" + brand), 0, head, 4, 8);
    return head;
  }

  @Test
  void theExtensionsResolveToTheirTypes() {
    assertThat(AllowedFileType.fromFileName("note.TXT")).contains(AllowedFileType.TXT);
    assertThat(AllowedFileType.fromFileName("letter.rtf")).contains(AllowedFileType.RTF);
    assertThat(AllowedFileType.fromFileName("photo.heic")).contains(AllowedFileType.HEIC);
    assertThat(AllowedFileType.fromFileName("photo.heif")).contains(AllowedFileType.HEIC);
    assertThat(AllowedFileType.fromFileName("a.gif")).contains(AllowedFileType.GIF);
    assertThat(AllowedFileType.fromFileName("a.bmp")).contains(AllowedFileType.BMP);
    assertThat(AllowedFileType.fromFileName("scan.tif")).contains(AllowedFileType.TIFF);
    assertThat(AllowedFileType.fromFileName("scan.tiff")).contains(AllowedFileType.TIFF);
    assertThat(AllowedFileType.fromFileName("a.webp")).contains(AllowedFileType.WEBP);
    assertThat(AllowedFileType.allowedExtensions()).contains("txt", "rtf", "heic", "webp", "tiff");
    assertThat(AllowedFileType.HEIC.mimeType()).isEqualTo("image/heic");
  }

  @Test
  void theContentMustMatchTheType() {
    assertThat(AllowedFileType.TXT.matches(ascii("Call back after 3 pm"))).isTrue();
    assertThat(AllowedFileType.TXT.matches(new byte[] {'M', 'Z', 0, 0})).isFalse();
    assertThat(AllowedFileType.RTF.matches(ascii("{\\rtf1\\ansi hello}"))).isTrue();
    assertThat(AllowedFileType.RTF.matches(ascii("%PDF-1.7"))).isFalse();
    assertThat(AllowedFileType.GIF.matches(ascii("GIF89a"))).isTrue();
    assertThat(AllowedFileType.BMP.matches(ascii("BM6"))).isTrue();
    assertThat(AllowedFileType.BMP.matches(ascii("MZ"))).isFalse();
  }

  @Test
  void heifPhotosAreRecognisedByTheirBrand() {
    assertThat(AllowedFileType.HEIC.matches(iso("heic"))).isTrue();
    assertThat(AllowedFileType.HEIC.matches(iso("mif1"))).isTrue();
    assertThat(AllowedFileType.HEIC.matches(iso("isom"))).isFalse();
    assertThat(AllowedFileType.HEIC.matches(ascii("ftyp"))).isFalse();
    assertThat(AllowedFileType.HEIC.matches(new byte[6])).isFalse();
  }

  @Test
  void tiffAndWebpAreRecognisedInBothForms() {
    assertThat(AllowedFileType.TIFF.matches(new byte[] {'I', 'I', '*', 0, 8})).isTrue();
    assertThat(AllowedFileType.TIFF.matches(new byte[] {'M', 'M', 0, '*', 0})).isTrue();
    assertThat(AllowedFileType.TIFF.matches(new byte[] {'M', 'M', '*', 0})).isFalse();
    assertThat(AllowedFileType.WEBP.matches(ascii("RIFF\u0000\u0000\u0000\u0000WEBPVP8 "))).isTrue();
    assertThat(AllowedFileType.WEBP.matches(ascii("RIFF\u0000\u0000\u0000\u0000WAVEfmt "))).isFalse();
    assertThat(AllowedFileType.WEBP.matches(ascii("RIFF"))).isFalse();
  }
}
