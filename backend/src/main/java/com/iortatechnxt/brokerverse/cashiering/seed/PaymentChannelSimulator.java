package com.iortatechnxt.brokerverse.cashiering.seed;

import com.iortatechnxt.brokerverse.cashiering.domain.ChannelProfile;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelProfileRepository;
import com.iortatechnxt.brokerverse.cashiering.service.MftIntakeService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The bank and channel simulator of the SIT and UAT environments (seed profile only): writes
 * payment files in BDOI's layouts of Appendix A and D (Bills Payment of OBPCS with its header
 * record, CLPC workbook with its TOTAL row, Trade text aligned in columns, Direct Credit
 * identifying the rows of a Bills Payment file, matured post-dated checks of PMS, Commission
 * Schedule) for booked accounts with an outstanding balance, one account not yet booked and one
 * unknown reference, and drops them in the MFT folder of their type or hands them for upload, so
 * that the intake runs end to end before BDOI IT connects the real channels.
 */
@Component
@Profile("seed")
public class PaymentChannelSimulator {

  private static final int TEXT_CAPACITY = 4096;

  private static final DateTimeFormatter US =
      DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ROOT);
  private static final DateTimeFormatter COMPACT =
      DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);
  private static final String BOOKED =
      "select i.invoice_no, coalesce(i.assured_name, 'Walk-in Payor') as name, i.insurer_code, sum(c.balance) as due"
          + " from ops_invoice i join ops_invoice_component c on c.invoice_id = i.id"
          + " where i.company_id = ? and i.payment_status in ('UNPAID', 'PARTIALLY_PAID')"
          + " and c.component in ('BASIC', 'DST', 'PREMIUM_TAX_VAT', 'LGT', 'FST', 'OTHER')"
          + " group by i.invoice_no, i.assured_name, i.insurer_code having sum(c.balance) > 0"
          + " order by i.invoice_no desc limit ?";
  private static final String PREBOOKED =
      "select a.arn from acc_account a where a.company_id = ? and a.arn is not null"
          + " and not exists (select 1 from ops_invoice i where i.arn = a.arn) order by a.id desc limit 1";

  private final ChannelProfileRepository profiles;
  private final MftIntakeService mft;
  private final JdbcTemplate jdbc;
  private final Clock clock;

  /**
   * Creates the simulator.
   *
   * @param profiles profiles of the file types (MFT folders)
   * @param mft MFT folders
   * @param jdbc accounts to pay
   * @param clock clock
   */
  public PaymentChannelSimulator(
      ChannelProfileRepository profiles, MftIntakeService mft, JdbcTemplate jdbc, Clock clock) {
    this.profiles = profiles;
    this.mft = mft;
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /**
   * Writes a file of a type.
   *
   * @param companyId company
   * @param fileType payment file type
   * @param rows booked accounts to pay (one pre-booked and one unknown reference are added)
   * @param billsFile for Direct Credit, the Bills Payment file whose rows it identifies
   * @return file name and content
   */
  public SimulatedFile generate(Long companyId, String fileType, int rows, String billsFile) {
    LocalDate day = BusinessClock.today(clock);
    List<Payee> payees = payees(companyId, Math.max(1, rows));
    return switch (fileType) {
      case "BILLS_PAYMENT" -> bills(day, payees);
      case "CLPC" -> clpc(day, payees);
      case "TRADE" -> trade(day, payees);
      case "DIRECT_CREDIT" -> directCredit(day, payees, billsFile);
      case "PDC" -> pdc(day, payees);
      case "COMMISSION_SCHEDULE" -> commission(day, payees);
      default -> throw new ResourceNotFoundException("Payment file type", fileType);
    };
  }

  /**
   * Writes a file and drops it in the MFT folder of its type.
   *
   * @param companyId company
   * @param fileType payment file type
   * @param rows booked accounts to pay
   * @return the file dropped
   */
  public Path drop(Long companyId, String fileType, int rows) {
    ChannelProfile profile =
        profiles
            .findById(fileType)
            .orElseThrow(() -> new ResourceNotFoundException("Payment file type", fileType));
    if (profile.getMftFolder() == null) {
      throw new BusinessRuleException("MFT_NO_FOLDER", profile.getName() + " has no MFT folder");
    }
    SimulatedFile file = generate(companyId, fileType, rows, null);
    try {
      Path folder = mft.folder(profile);
      Files.createDirectories(folder);
      return Files.write(folder.resolve(file.name()), file.content());
    } catch (IOException ex) {
      throw new UncheckedIOException("The MFT folder cannot be written", ex);
    }
  }

  private List<Payee> payees(Long companyId, int rows) {
    List<Payee> payees = new ArrayList<>();
    jdbc.query(
        BOOKED,
        rs -> {
          payees.add(
              new Payee(
                  rs.getString("invoice_no"),
                  rs.getString("name"),
                  rs.getString("insurer_code"),
                  rs.getBigDecimal("due")));
        },
        companyId,
        rows);
    jdbc.query(
        PREBOOKED,
        rs -> {
          payees.add(
              new Payee(rs.getString("arn"), "Pre-booked Payor", null, new BigDecimal("1500.00")));
        },
        companyId);
    payees.add(
        new Payee(
            "UNKNOWN-" + BusinessClock.today(clock).format(COMPACT),
            "Unidentified Payor",
            null,
            new BigDecimal("750.25")));
    return payees;
  }

  private SimulatedFile bills(LocalDate day, List<Payee> payees) {
    StringBuilder text = new StringBuilder();
    BigDecimal total = payees.stream().map(Payee::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    text.append(
        String.format(Locale.ROOT, "1|%05d|%012.2f|%s%n", payees.size(), total, day.format(US)));
    int n = 0;
    for (Payee p : payees) {
      n++;
      text.append(
          String.format(
              Locale.ROOT,
              "2|%05d|%s|%s|%012.2f|N|2|BDO|00411335|||%s||00300|%s|10:15:00%n",
              n,
              p.reference(),
              p.name(),
              p.amount(),
              p.reference(),
              day.format(US)));
    }
    return new SimulatedFile("BDOI" + day.format(COMPACT) + "1.TXT", bytes(text));
  }

  private SimulatedFile clpc(LocalDate day, List<Payee> payees) {
    try (XSSFWorkbook book = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = book.createSheet("CLPC");
      String[] head = {
        "COUNT",
        "DATE CREDIT",
        "AMOUNT",
        "AR NUMBER",
        "INVOICE",
        "ASSURED",
        "PN NUMBER",
        "CORP.DEPT",
        "RISK CODE",
        "REMARKS"
      };
      Row header = sheet.createRow(0);
      for (int c = 0; c < head.length; c++) {
        header.createCell(c).setCellValue(head[c]);
      }
      BigDecimal total = BigDecimal.ZERO;
      int r = 1;
      for (Payee p : payees) {
        Row row = sheet.createRow(r);
        String[] cells = {
          String.valueOf(r),
          day.format(US),
          p.amount().toPlainString(),
          "",
          p.reference(),
          p.name(),
          "",
          "SMALLBUSINESSLOAN",
          "FIP"
        };
        for (int c = 0; c < cells.length; c++) {
          row.createCell(c).setCellValue(cells[c]);
        }
        total = total.add(p.amount());
        r++;
      }
      Row totals = sheet.createRow(r);
      totals.createCell(0).setCellValue("TOTAL");
      totals.createCell(2).setCellValue(total.toPlainString());
      book.write(out);
      String month = day.format(DateTimeFormatter.ofPattern("MMMddyyyy", Locale.ENGLISH));
      return new SimulatedFile("CLPC-" + month + ".xlsx", out.toByteArray());
    } catch (IOException ex) {
      throw new UncheckedIOException("The CLPC workbook cannot be written", ex);
    }
  }

  private SimulatedFile trade(LocalDate day, List<Payee> payees) {
    StringBuilder text = new StringBuilder(TEXT_CAPACITY);
    text.append(
        "Transaction Date  Branch Name  Transaction Description  Debit  Credit  Running Balance  Check No.\n");
    BigDecimal running = BigDecimal.ZERO;
    for (Payee p : payees) {
      running = running.add(p.amount());
      text.append(day.format(US))
          .append("  RECON  FIP/")
          .append(p.reference())
          .append("/FSA7041259859IVF  0  ")
          .append(p.amount().toPlainString())
          .append("  ")
          .append(running.toPlainString())
          .append("  0\n");
    }
    return new SimulatedFile(
        "UploadTrade_"
            + day.format(DateTimeFormatter.ofPattern("MM.dd.yyyy", Locale.ROOT))
            + ".TXT",
        bytes(text));
  }

  private SimulatedFile directCredit(LocalDate day, List<Payee> payees, String billsFile) {
    String bp = billsFile == null ? "BDOI" + day.format(COMPACT) + "1.TXT" : billsFile;
    StringBuilder text =
        new StringBuilder(
            "Transaction date|BP filename|Transaction no|Paid amount|Payment type|Payor"
                + "|Account ref no|Assured|Ebix reference no.|Logged by|Requestor\n");
    int n = 0;
    for (Payee p : payees) {
      n++;
      text.append(
          String.format(
              Locale.ROOT,
              "%s|%s|%05d|%s|ONLINE|%s|%s|%s||Cashier|Collections%n",
              day.format(US),
              bp,
              n,
              p.amount().toPlainString(),
              p.name(),
              p.reference(),
              p.name()));
    }
    return new SimulatedFile("OTC" + day.format(COMPACT) + "1- AUTOCREDIT.TXT", bytes(text));
  }

  private SimulatedFile pdc(LocalDate day, List<Payee> payees) {
    StringBuilder text =
        new StringBuilder(
            "Client code|Payor|Reference|Check number|Bank code|Check branch|Maturity date|Amount|Market segment\n");
    int n = 0;
    for (Payee p : payees) {
      n++;
      text.append(
          String.format(
              Locale.ROOT,
              "|%s|%s|PMS%s%03d|PNB|Makati|%s|%s|CBG%n",
              p.name(),
              p.reference(),
              day.format(COMPACT),
              n,
              day.format(US),
              p.amount().toPlainString()));
    }
    return new SimulatedFile("BDOI" + day.format(COMPACT) + "1PMS.TXT", bytes(text));
  }

  private SimulatedFile commission(LocalDate day, List<Payee> payees) {
    StringBuilder text =
        new StringBuilder(
            "Insurer code|Payee name|Certificate ref|Payment ref|Invoice no|Basic commission"
                + "|VAT|Withholding tax|Payment date\n");
    int n = 0;
    for (Payee p : payees) {
      if (p.insurer() == null) {
        continue;
      }
      n++;
      text.append(
          String.format(
              Locale.ROOT,
              "%s|Insurer %s|2307-SIM-%d|CHK-SIM-%d|%s|1000.00|120.00|20.00|%s%n",
              p.insurer(),
              p.insurer(),
              n,
              n,
              p.reference(),
              day.format(US)));
    }
    return new SimulatedFile(
        "CommissionSchedule_"
            + day.format(DateTimeFormatter.ofPattern("MMddyyyy", Locale.ROOT))
            + ".csv",
        bytes(text));
  }

  private static byte[] bytes(StringBuilder text) {
    return text.toString().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * An account the simulated file pays.
   *
   * @param reference invoice, ARN or unknown reference
   * @param name payor
   * @param insurer insurer of the account, may be null
   * @param amount amount paid
   */
  record Payee(String reference, String name, String insurer, BigDecimal amount) {}

  /**
   * A simulated file.
   *
   * @param name file name in BDOI's convention
   * @param content bytes
   */
  public record SimulatedFile(String name, byte[] content) {

    /** Defensive copy. */
    public SimulatedFile {
      content = content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof SimulatedFile f
          && name.equals(f.name)
          && java.util.Arrays.equals(content, f.content);
    }

    @Override
    public int hashCode() {
      return 31 * name.hashCode() + java.util.Arrays.hashCode(content);
    }

    @Override
    public String toString() {
      return "SimulatedFile[" + name + ", " + content.length + " bytes]";
    }
  }
}
