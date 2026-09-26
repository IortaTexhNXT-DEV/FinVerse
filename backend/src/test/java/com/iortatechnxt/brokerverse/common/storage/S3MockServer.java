package com.iortatechnxt.brokerverse.common.storage;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A minimal in-process S3 REST service (path-style) for the {@link S3FileStore} test when no
 * container runtime is available: put (plain or aws-chunked body), get, head, delete, copy, object
 * tagging, object legal hold and list-objects-v2. It records the request headers of every write so
 * the test can check encryption and checksum headers. Not a full S3 implementation.
 */
final class S3MockServer implements AutoCloseable {

  /** One stored object. */
  static final class Obj {
    private byte[] content;
    private String contentType;
    private final String versionId = UUID.randomUUID().toString();
    private final Instant lastModified = Instant.now();
    private Map<String, String> tags = new HashMap<>();
    private boolean legalHold;
  }

  private static final Pattern CHUNK_HEADER = Pattern.compile("^([0-9a-fA-F]+)(;.*)?$");
  private static final DateTimeFormatter HTTP_DATE =
      DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);

  private final Map<String, Obj> objects = new ConcurrentHashMap<>();
  private final List<Map<String, String>> writeHeaders = new ArrayList<>();
  private final HttpServer server;

  S3MockServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext("/", this::handle);
    server.start();
  }

  /** Content of an object ({@code bucket/key}), null when absent. */
  byte[] content(String id) {
    Obj obj = objects.get(id);
    return obj == null ? null : obj.content;
  }

  /** Sets a tag, as the malware scanning service does. */
  void tag(String id, String name, String value) {
    objects.get(id).tags.put(name, value);
  }

  /** Headers of the last write (put or copy), names in lower case. */
  Map<String, String> lastWriteHeaders() {
    return writeHeaders.get(writeHeaders.size() - 1);
  }

  URI endpoint() {
    return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
  }

  @Override
  public void close() {
    server.stop(0);
  }

  private void handle(HttpExchange ex) throws IOException {
    try {
      route(ex);
    } catch (RuntimeException e) {
      send(ex, 500, error("InternalError", e.toString()));
    } finally {
      ex.close();
    }
  }

  private void route(HttpExchange ex) throws IOException {
    String path = ex.getRequestURI().getRawPath();
    String query = ex.getRequestURI().getRawQuery() == null ? "" : ex.getRequestURI().getRawQuery();
    String[] parts = path.substring(1).split("/", 2);
    String bucket = parts[0];
    String key = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
    String id = bucket + "/" + key;
    String method = ex.getRequestMethod();
    if (key.isEmpty() && "GET".equals(method)) {
      list(ex, bucket, params(query).getOrDefault("prefix", ""));
      return;
    }
    switch (method) {
      case "PUT" -> put(ex, id, query);
      case "GET" -> get(ex, id, query);
      case "HEAD" -> head(ex, id);
      case "DELETE" -> {
        objects.remove(id);
        send(ex, 204, null);
      }
      default -> send(ex, 405, error("MethodNotAllowed", method));
    }
  }

  private void put(HttpExchange ex, String id, String query) throws IOException {
    byte[] body = body(ex);
    if (query.contains("legal-hold")) {
      Obj obj = objects.get(id);
      obj.legalHold = new String(body, StandardCharsets.UTF_8).contains(">ON<");
      send(ex, 200, null);
      return;
    }
    writeHeaders.add(headers(ex));
    String copySource = ex.getRequestHeaders().getFirst("x-amz-copy-source");
    Obj obj = new Obj();
    if (copySource != null) {
      String source = URLDecoder.decode(copySource, StandardCharsets.UTF_8).replaceFirst("^/", "");
      Obj from = objects.get(source);
      obj.content = from.content.clone();
      obj.contentType = from.contentType;
      obj.tags = new HashMap<>(from.tags);
      objects.put(id, obj);
      ex.getResponseHeaders().add("x-amz-version-id", obj.versionId);
      send(
          ex,
          200,
          "<CopyObjectResult><ETag>\"e\"</ETag><LastModified>"
              + obj.lastModified
              + "</LastModified></CopyObjectResult>");
      return;
    }
    obj.content = body;
    obj.contentType = ex.getRequestHeaders().getFirst("Content-Type");
    objects.put(id, obj);
    ex.getResponseHeaders().add("x-amz-version-id", obj.versionId);
    ex.getResponseHeaders().add("ETag", "\"e\"");
    send(ex, 200, null);
  }

  private void get(HttpExchange ex, String id, String query) throws IOException {
    Obj obj = objects.get(id);
    if (obj == null) {
      send(ex, 404, error("NoSuchKey", "The specified key does not exist."));
      return;
    }
    if (query.contains("tagging")) {
      StringBuilder xml = new StringBuilder("<Tagging><TagSet>");
      obj.tags.forEach(
          (k, v) ->
              xml.append("<Tag><Key>")
                  .append(k)
                  .append("</Key><Value>")
                  .append(v)
                  .append("</Value></Tag>"));
      send(ex, 200, xml.append("</TagSet></Tagging>").toString());
      return;
    }
    ex.getResponseHeaders().add("Content-Type", obj.contentType);
    ex.getResponseHeaders().add("x-amz-version-id", obj.versionId);
    ex.sendResponseHeaders(200, obj.content.length);
    try (OutputStream out = ex.getResponseBody()) {
      out.write(obj.content);
    }
  }

  private void head(HttpExchange ex, String id) throws IOException {
    Obj obj = objects.get(id);
    if (obj == null) {
      ex.sendResponseHeaders(404, -1);
      return;
    }
    ex.getResponseHeaders().add("Content-Type", obj.contentType);
    ex.getResponseHeaders().add("Last-Modified", HTTP_DATE.format(obj.lastModified));
    ex.getResponseHeaders().add("x-amz-version-id", obj.versionId);
    ex.getResponseHeaders().add("x-amz-object-lock-legal-hold", obj.legalHold ? "ON" : "OFF");
    ex.getResponseHeaders().add("Content-Length", Integer.toString(obj.content.length));
    ex.sendResponseHeaders(200, -1);
  }

  private void list(HttpExchange ex, String bucket, String prefix) throws IOException {
    StringBuilder xml =
        new StringBuilder("<ListBucketResult><Name>")
            .append(bucket)
            .append("</Name><IsTruncated>false</IsTruncated>");
    new TreeMap<>(objects)
        .forEach(
            (id, obj) -> {
              String key = id.substring(bucket.length() + 1);
              if (id.startsWith(bucket + "/") && key.startsWith(prefix)) {
                xml.append("<Contents><Key>")
                    .append(key)
                    .append("</Key><LastModified>")
                    .append(obj.lastModified)
                    .append("</LastModified><Size>")
                    .append(obj.content.length)
                    .append("</Size></Contents>");
              }
            });
    send(ex, 200, xml.append("</ListBucketResult>").toString());
  }

  private static Map<String, String> params(String query) {
    Map<String, String> params = new LinkedHashMap<>();
    for (String pair : query.split("&")) {
      int eq = pair.indexOf('=');
      if (eq > 0) {
        params.put(
            pair.substring(0, eq),
            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
      }
    }
    return params;
  }

  private static Map<String, String> headers(HttpExchange ex) {
    Map<String, String> headers = new HashMap<>();
    ex.getRequestHeaders().forEach((k, v) -> headers.put(k.toLowerCase(), String.join(",", v)));
    return headers;
  }

  /** The request body; aws-chunked bodies are decoded (chunk signatures and trailers dropped). */
  private static byte[] body(HttpExchange ex) throws IOException {
    byte[] raw;
    try (InputStream in = ex.getRequestBody()) {
      raw = in.readAllBytes();
    }
    String encoding = ex.getRequestHeaders().getFirst("Content-Encoding");
    String sha = ex.getRequestHeaders().getFirst("x-amz-content-sha256");
    boolean chunked =
        encoding != null && encoding.contains("aws-chunked")
            || sha != null && sha.startsWith("STREAMING-");
    return chunked ? dechunk(raw) : raw;
  }

  private static byte[] dechunk(byte[] raw) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int pos = 0;
    while (pos < raw.length) {
      int lineEnd = indexOfCrlf(raw, pos);
      String header = new String(raw, pos, lineEnd - pos, StandardCharsets.US_ASCII);
      Matcher m = CHUNK_HEADER.matcher(header);
      if (!m.matches()) {
        break;
      }
      int size = Integer.parseInt(m.group(1), 16);
      if (size == 0) {
        break;
      }
      out.write(raw, lineEnd + 2, size);
      pos = lineEnd + 2 + size + 2;
    }
    return out.toByteArray();
  }

  private static int indexOfCrlf(byte[] raw, int from) {
    for (int i = from; i < raw.length - 1; i++) {
      if (raw[i] == '\r' && raw[i + 1] == '\n') {
        return i;
      }
    }
    return raw.length;
  }

  private static String error(String code, String message) {
    return "<Error><Code>" + code + "</Code><Message>" + message + "</Message></Error>";
  }

  private static void send(HttpExchange ex, int status, String xml) throws IOException {
    if (xml == null) {
      ex.sendResponseHeaders(status, -1);
      return;
    }
    byte[] body = xml.getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().add("Content-Type", "application/xml");
    ex.sendResponseHeaders(status, body.length);
    try (OutputStream out = ex.getResponseBody()) {
      out.write(body);
    }
  }
}
