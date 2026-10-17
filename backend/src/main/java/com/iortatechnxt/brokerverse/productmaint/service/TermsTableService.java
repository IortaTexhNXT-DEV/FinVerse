package com.iortatechnxt.brokerverse.productmaint.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.productmaint.domain.FinalTerm;
import com.iortatechnxt.brokerverse.productmaint.domain.FinalTermChange;
import com.iortatechnxt.brokerverse.productmaint.domain.FinalTermChangeRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.FinalTermRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.TermsOption;
import com.iortatechnxt.brokerverse.productmaint.domain.TermsOptionRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.TermsRecord;
import com.iortatechnxt.brokerverse.productmaint.domain.TermsSetting;
import com.iortatechnxt.brokerverse.productmaint.domain.TermsSettingRepository;
import com.iortatechnxt.brokerverse.productmaint.service.TermsSources.Facts;
import com.iortatechnxt.brokerverse.productmaint.service.TermsSources.InsurerTerms;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The comparative table of BDOI's FRS (FRPM.006.02, FRPM.009.01, FRPM.012.02): the chosen fields as
 * rows, the QS value of each field, one column per quotation option of each insurer (several
 * options per insurer) with the insurer response Approved, Not Covered or Others, and the Final
 * Terms for Proposal column whose every change is kept in the history. The terms of an insurer's
 * response show as option 1 until an option 1 is keyed in the table.
 */
@Service
@Transactional
public class TermsTableService {

  /** Insurer response values. */
  public static final Set<String> ANSWERS = Set.of("APPROVED", "NOT_COVERED", "OTHERS");

  private static final TypeReference<Map<String, String>> VALUES = new TypeReference<>() {};

  private final TermsSources sources;
  private final TermsOptionRepository options;
  private final TermsSettingRepository settings;
  private final FinalTermRepository finals;
  private final FinalTermChangeRepository changes;
  private final ObjectMapper json;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param sources record facts and insurer responses
   * @param options quotation options
   * @param settings choices of the table
   * @param finals Final Terms for Proposal
   * @param changes history of the Final Terms
   * @param json JSON of the option values
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public TermsTableService(
      TermsSources sources,
      TermsOptionRepository options,
      TermsSettingRepository settings,
      FinalTermRepository finals,
      FinalTermChangeRepository changes,
      ObjectMapper json,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.sources = sources;
    this.options = options;
    this.settings = settings;
    this.finals = finals;
    this.changes = changes;
    this.json = json;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The table of a record.
   *
   * @param record the record
   * @return the table
   */
  @Transactional(readOnly = true)
  public TermsTable table(TermsRecord record) {
    Facts facts = sources.facts(record);
    TermsSetting setting = setting(record);
    List<String> shown =
        setting.getDisplayFields().isEmpty() ? TermsField.keys() : setting.getDisplayFields();
    Map<String, String> qs = new LinkedHashMap<>();
    facts.qsValues().forEach((k, v) -> qs.put(k.name(), v));
    return new TermsTable(
        facts.requestNo(),
        TermsField.keys(),
        shown,
        setting.getClientFields().isEmpty() ? shown : setting.getClientFields(),
        qs,
        columns(record, facts),
        finalTerms(record, facts, setting),
        setting.getSelectedInsurers());
  }

  private List<OptionColumn> columns(TermsRecord record, Facts facts) {
    List<TermsOption> saved =
        options.findByRecordTypeAndRecordIdOrderByInsurerCodeAscOptionNoAsc(
            record.type(), record.id());
    List<OptionColumn> out = new ArrayList<>();
    for (InsurerTerms insurer : facts.insurers()) {
      List<TermsOption> own =
          saved.stream().filter(o -> o.getInsurerCode().equals(insurer.insurerCode())).toList();
      if (own.stream().noneMatch(o -> o.getOptionNo() == 1)) {
        Map<String, String> values = new LinkedHashMap<>();
        insurer.values().forEach((k, v) -> values.put(k.name(), v));
        out.add(
            new OptionColumn(
                insurer.insurerCode(),
                insurer.insurerName(),
                1,
                insurer.responded() ? insurer.answer() : null,
                null,
                values,
                false));
      }
      own.forEach(o -> out.add(column(o)));
    }
    return out;
  }

  private OptionColumn column(TermsOption o) {
    return new OptionColumn(
        o.getInsurerCode(),
        o.getInsurerName(),
        o.getOptionNo(),
        o.getAnswer(),
        o.getOtherAnswer(),
        read(o.getOptionValues()),
        true);
  }

  private Map<String, String> finalTerms(TermsRecord record, Facts facts, TermsSetting setting) {
    Map<String, String> out = new LinkedHashMap<>();
    List<FinalTerm> saved = finals.findByRecordTypeAndRecordId(record.type(), record.id());
    if (saved.isEmpty() && !setting.getSelectedInsurers().isEmpty()) {
      String first = setting.getSelectedInsurers().get(0);
      columns(record, facts).stream()
          .filter(c -> c.insurerCode().equals(first))
          .findFirst()
          .ifPresent(c -> out.putAll(c.values()));
      return out;
    }
    saved.forEach(f -> out.put(f.getFieldKey(), f.getFieldValue()));
    return out;
  }

  /**
   * Chooses the fields shown in the table and sent to the client.
   *
   * @param record the record
   * @param shown fields shown
   * @param client fields sent to the client
   * @return the table
   */
  public TermsTable fields(TermsRecord record, List<String> shown, List<String> client) {
    checkFields(shown);
    checkFields(client);
    TermsSetting setting = settingFor(record);
    setting.fields(shown, client);
    return table(record);
  }

  private static void checkFields(List<String> keys) {
    if (keys != null && !keys.stream().allMatch(TermsField::known)) {
      throw new BusinessRuleException("PM_TERMS_FIELD", "Choose fields of the comparative table");
    }
  }

  /**
   * Adds or changes a quotation option of an insurer.
   *
   * @param record the record
   * @param insurerCode insurer
   * @param optionNo option number, null for a new option
   * @param input response and values
   * @return the table
   */
  public TermsTable saveOption(
      TermsRecord record, String insurerCode, Integer optionNo, OptionInput input) {
    checkAnswer(input);
    Facts facts = sources.facts(record);
    InsurerTerms insurer =
        facts.insurers().stream()
            .filter(i -> i.insurerCode().equals(insurerCode))
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "PM_TERMS_INSURER", insurerCode + " is not an insurer of this request"));
    List<TermsOption> own =
        options
            .findByRecordTypeAndRecordIdOrderByInsurerCodeAscOptionNoAsc(record.type(), record.id())
            .stream()
            .filter(o -> o.getInsurerCode().equals(insurerCode))
            .toList();
    TermsOption option =
        optionNo == null
            ? null
            : own.stream().filter(o -> o.getOptionNo() == optionNo).findFirst().orElse(null);
    if (option == null) {
      int next = own.stream().mapToInt(TermsOption::getOptionNo).max().orElse(1) + 1;
      option =
          options.save(
              new TermsOption(
                  record, insurerCode, insurer.insurerName(), optionNo == null ? next : optionNo));
    }
    option.change(input.answer(), input.otherAnswer(), write(input.values()));
    audit.record(
        record.type(),
        facts.requestNo(),
        AuditAction.UPDATE,
        "Insurer option " + option.getOptionNo() + " of " + insurer.insurerName() + " saved");
    return table(record);
  }

  private static void checkAnswer(OptionInput input) {
    if (!ANSWERS.contains(input.answer())) {
      throw new BusinessRuleException(
          "PM_TERMS_ANSWER", "The insurer response is Approved, Not Covered or Others");
    }
    if ("OTHERS".equals(input.answer())
        && (input.otherAnswer() == null || input.otherAnswer().isBlank())) {
      throw new BusinessRuleException(
          "PM_TERMS_OTHER", "Specify the insurer's actual response for Others");
    }
    checkFields(List.copyOf(input.values().keySet()));
  }

  /**
   * Removes a quotation option.
   *
   * @param record the record
   * @param insurerCode insurer
   * @param optionNo option
   * @return the table
   */
  public TermsTable removeOption(TermsRecord record, String insurerCode, int optionNo) {
    options
        .findByRecordTypeAndRecordIdOrderByInsurerCodeAscOptionNoAsc(record.type(), record.id())
        .stream()
        .filter(o -> o.getInsurerCode().equals(insurerCode) && o.getOptionNo() == optionNo)
        .findFirst()
        .ifPresent(options::delete);
    return table(record);
  }

  /**
   * Changes the Final Terms for Proposal; every changed value is kept in the history.
   *
   * @param record the record
   * @param values new value by field
   * @return the table
   */
  public TermsTable saveFinalTerms(TermsRecord record, Map<String, String> values) {
    checkFields(List.copyOf(values.keySet()));
    Map<String, String> before = table(record).finalTerms();
    Map<String, FinalTerm> saved = new LinkedHashMap<>();
    finals
        .findByRecordTypeAndRecordId(record.type(), record.id())
        .forEach(f -> saved.put(f.getFieldKey(), f));
    String user = currentUser.username();
    for (Map.Entry<String, String> e : values.entrySet()) {
      String value = e.getValue() == null || e.getValue().isBlank() ? null : e.getValue().strip();
      FinalTerm term =
          saved.computeIfAbsent(e.getKey(), k -> finals.save(new FinalTerm(record, k)));
      String old = before.get(e.getKey());
      if (!Objects.equals(old, value)) {
        changes.save(
            new FinalTermChange(
                record, e.getKey(), new String[] {old, value}, user, clock.instant()));
      }
      term.change(value);
    }
    return table(record);
  }

  /**
   * The history of the Final Terms, newest first.
   *
   * @param record the record
   * @return changes
   */
  @Transactional(readOnly = true)
  public List<FinalTermChange> history(TermsRecord record) {
    return changes.findByRecordTypeAndRecordIdOrderByIdDesc(record.type(), record.id());
  }

  /**
   * Records the insurers selected for the proposal.
   *
   * @param record the record
   * @param insurers insurer codes
   * @return the choices
   */
  public TermsSetting select(TermsRecord record, List<String> insurers) {
    TermsSetting setting = settingFor(record);
    setting.select(insurers, currentUser.username(), clock.instant());
    return setting;
  }

  private TermsSetting setting(TermsRecord record) {
    return settings
        .findByRecordTypeAndRecordId(record.type(), record.id())
        .orElseGet(() -> new TermsSetting(record));
  }

  private TermsSetting settingFor(TermsRecord record) {
    return settings
        .findByRecordTypeAndRecordId(record.type(), record.id())
        .orElseGet(() -> settings.save(new TermsSetting(record)));
  }

  private Map<String, String> read(String text) {
    try {
      return json.readValue(text, VALUES);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Option values cannot be read", e);
    }
  }

  private String write(Map<String, String> values) {
    try {
      return json.writeValueAsString(values);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Option values cannot be written", e);
    }
  }

  /**
   * The comparative table.
   *
   * @param requestNo request number
   * @param fields every field
   * @param shown the fields shown (rows)
   * @param clientFields the fields sent to the client
   * @param qsValues the QS value of each field
   * @param columns one column per quotation option of each insurer
   * @param finalTerms the Final Terms for Proposal
   * @param selectedInsurers the insurers selected for the proposal
   */
  public record TermsTable(
      String requestNo,
      List<String> fields,
      List<String> shown,
      List<String> clientFields,
      Map<String, String> qsValues,
      List<OptionColumn> columns,
      Map<String, String> finalTerms,
      List<String> selectedInsurers) {}

  /**
   * A quotation option of an insurer.
   *
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param optionNo option number
   * @param answer APPROVED, NOT_COVERED or OTHERS; null while the insurer has not responded
   * @param otherAnswer the insurer's own response (Others)
   * @param values the value of each field
   * @param saved whether the option was keyed in the table (else taken from the response)
   */
  public record OptionColumn(
      String insurerCode,
      String insurerName,
      int optionNo,
      String answer,
      String otherAnswer,
      Map<String, String> values,
      boolean saved) {}

  /**
   * A quotation option as keyed.
   *
   * @param answer APPROVED, NOT_COVERED or OTHERS
   * @param otherAnswer the insurer's own response (Others)
   * @param values the value of each field
   */
  public record OptionInput(String answer, String otherAnswer, Map<String, String> values) {

    /** Defensive copy. */
    public OptionInput {
      values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
  }
}
