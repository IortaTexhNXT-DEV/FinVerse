package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The profile of a payment file type (FRS.CSH.05.01.06; Appendix D): the file name convention, the
 * format of BDOI's file (separator, header line, header and detail record types, TOTAL row, date
 * format), the map of BDOI's fields to the fields of the system, the size limit and the MFT folder.
 * Every element is changed in Cashiering Setup without a change to the system.
 */
@Entity
@Table(name = "csh_channel_profile")
public class ChannelProfile {

  @Id
  @Column(name = "file_type", length = 30)
  private String fileType;

  @Version private long version;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(name = "handler_code", nullable = false, length = 40)
  private String handlerCode;

  @Column(name = "name_pattern", nullable = false, length = 200)
  private String namePattern;

  @Column(name = "name_example", nullable = false, length = 80)
  private String nameExample;

  @Column(name = "file_format", nullable = false, length = 10)
  private String fileFormat;

  @Column(nullable = false, length = 5)
  private String delimiter;

  @Column(name = "header_line", nullable = false)
  private boolean headerLine;

  @Column(name = "header_record", length = 5)
  private String headerRecord;

  @Column(name = "detail_record", length = 5)
  private String detailRecord;

  @Column(name = "total_row_label", length = 20)
  private String totalRowLabel;

  @Column(name = "date_format", nullable = false, length = 20)
  private String dateFormat;

  @Column(name = "field_map", nullable = false, length = 1000)
  private String fieldMap;

  @Column(name = "control_map", length = 200)
  private String controlMap;

  @Column(name = "max_mb", nullable = false)
  private int maxMb;

  @Column(name = "mft_folder", length = 300)
  private String mftFolder;

  @Column(name = "mft_enabled", nullable = false)
  private boolean mftEnabled;

  @Column(name = "source_system", nullable = false, length = 30)
  private String sourceSystem;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "updated_by", length = 50)
  private String updatedBy;

  protected ChannelProfile() {}

  /**
   * Changes the convention, the size limit and the MFT folder.
   *
   * @param change new values
   * @param by user
   * @param at time
   */
  public void change(Change change, String by, Instant at) {
    try {
      Pattern.compile(change.namePattern());
    } catch (PatternSyntaxException ex) {
      throw new BusinessRuleException(
          "CHANNEL_NAME_PATTERN", "The file name convention is not a valid pattern", ex);
    }
    fields(change.fieldMap());
    this.namePattern = change.namePattern();
    this.nameExample = change.nameExample();
    this.fieldMap = change.fieldMap();
    this.maxMb = change.maxMb();
    this.mftFolder = change.mftFolder();
    this.mftEnabled = change.mftEnabled();
    this.updatedBy = by;
    this.updatedAt = at;
  }

  /**
   * Whether a file name follows the convention.
   *
   * @param fileName file name
   * @return true when it matches
   */
  public boolean accepts(String fileName) {
    return Pattern.compile(namePattern).matcher(fileName.strip()).matches();
  }

  /**
   * The fields of the system and where each is found in BDOI's file (a position from 1 or a column
   * name).
   *
   * @return map in order
   */
  public Map<String, String> fields() {
    return fields(fieldMap);
  }

  /**
   * The control fields of the header record (count, total, date) and their positions.
   *
   * @return map, empty without a header record
   */
  public Map<String, String> controls() {
    return controlMap == null || controlMap.isBlank() ? Map.of() : fields(controlMap);
  }

  private static Map<String, String> fields(String text) {
    Map<String, String> map = new LinkedHashMap<>();
    for (String part : text.split(";")) {
      int eq = part.indexOf('=');
      if (eq <= 0 || eq == part.length() - 1) {
        throw new BusinessRuleException(
            "CHANNEL_FIELD_MAP",
            "Write each field as Field=position or Field=column, not '" + part + "'");
      }
      map.put(part.substring(0, eq).strip(), part.substring(eq + 1).strip());
    }
    return map;
  }

  public String getFileType() {
    return fileType;
  }

  public String getName() {
    return name;
  }

  public String getHandlerCode() {
    return handlerCode;
  }

  public String getNamePattern() {
    return namePattern;
  }

  public String getNameExample() {
    return nameExample;
  }

  public String getFileFormat() {
    return fileFormat;
  }

  public String getDelimiter() {
    return delimiter;
  }

  public boolean isHeaderLine() {
    return headerLine;
  }

  public String getHeaderRecord() {
    return headerRecord;
  }

  public String getDetailRecord() {
    return detailRecord;
  }

  public String getTotalRowLabel() {
    return totalRowLabel;
  }

  public String getDateFormat() {
    return dateFormat;
  }

  public String getFieldMap() {
    return fieldMap;
  }

  public int getMaxMb() {
    return maxMb;
  }

  public String getMftFolder() {
    return mftFolder;
  }

  public boolean isMftEnabled() {
    return mftEnabled;
  }

  public String getSourceSystem() {
    return sourceSystem;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  /**
   * A change of a profile.
   *
   * @param namePattern file name convention (regular expression)
   * @param nameExample example of a file name
   * @param fieldMap fields of the system and their place in BDOI's file
   * @param maxMb size limit in MB
   * @param mftFolder MFT folder, may be null
   * @param mftEnabled files are taken from the MFT folder
   */
  public record Change(
      String namePattern,
      String nameExample,
      String fieldMap,
      int maxMb,
      String mftFolder,
      boolean mftEnabled) {}
}
