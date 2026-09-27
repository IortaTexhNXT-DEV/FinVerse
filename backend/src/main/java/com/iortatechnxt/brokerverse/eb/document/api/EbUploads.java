package com.iortatechnxt.brokerverse.eb.document.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * Helpers of the EB multipart endpoints: the uploaded files and the JSON form field that carries
 * the structured part of a request sent with its files.
 */
public final class EbUploads {

  private EbUploads() {}

  /**
   * The uploaded files.
   *
   * @param files multipart files, may be null
   * @return files
   */
  public static List<UploadedFile> files(List<MultipartFile> files) {
    List<UploadedFile> result = new ArrayList<>();
    if (files != null) {
      for (MultipartFile f : files) {
        result.add(file(f));
      }
    }
    return result;
  }

  /**
   * One uploaded file.
   *
   * @param file multipart file, may be null
   * @return file, null when none
   */
  public static UploadedFile file(MultipartFile file) {
    if (file == null) {
      return null;
    }
    try {
      return new UploadedFile(file.getOriginalFilename(), file.getBytes());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Reads the JSON form field of a multipart request.
   *
   * @param json mapper
   * @param value field value
   * @param type target type
   * @param <T> type
   * @return value
   */
  public static <T> T read(ObjectMapper json, String value, Class<T> type) {
    try {
      return json.readValue(value == null || value.isBlank() ? "{}" : value, type);
    } catch (JsonProcessingException e) {
      throw new BusinessRuleException("EB_REQUEST_INVALID", "The request could not be read", e);
    }
  }
}
