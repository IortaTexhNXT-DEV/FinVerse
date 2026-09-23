package com.iortatechnxt.finverse.receivables.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Automatic bank reconciliation matching (pure algorithm).
 *
 * <ol>
 *   <li><b>One to one</b>: a bank line matches an unmatched book entry of the same signed amount
 *       whose date is within the window. Among candidates, an entry whose reference or narration
 *       contains the bank line reference (cheque / receipt number), or whose reference appears in
 *       the bank line, wins; then the closest date; then the oldest entry.
 *   <li><b>Group by reference</b>: a bank line whose reference is a deposit slip number matches the
 *       unmatched book entries of the slip's receipts; otherwise it matches the entries carrying
 *       the same reference (e.g. the applied and unapplied postings of one receipt) - in both cases
 *       when their total equals the bank line.
 * </ol>
 */
public final class AutoMatcher {

  private AutoMatcher() {}

  /**
   * Proposes matches.
   *
   * @param book unmatched book items
   * @param bank unmatched bank items
   * @param windowDays maximum days between book and bank date
   * @param slips deposit slip number to receipt numbers
   * @return proposals (each item used at most once)
   */
  public static List<Proposal> match(
      List<Item> book, List<Item> bank, int windowDays, Map<String, Set<String>> slips) {
    Set<Long> usedBook = new HashSet<>();
    List<Proposal> proposals = new ArrayList<>();
    List<Item> leftBank = new ArrayList<>();
    for (Item line : bank) {
      Optional<Item> best =
          book.stream()
              .filter(b -> !usedBook.contains(b.id()))
              .filter(b -> b.amount().compareTo(line.amount()) == 0)
              .filter(b -> days(b.date(), line.date()) <= windowDays)
              .min(preference(line));
      if (best.isPresent()) {
        usedBook.add(best.get().id());
        proposals.add(new Proposal(List.of(best.get().id()), List.of(line.id())));
      } else {
        leftBank.add(line);
      }
    }
    for (Item line : leftBank) {
      slipMatch(line, book, usedBook, windowDays, slips).ifPresent(proposals::add);
    }
    return proposals;
  }

  private static Optional<Proposal> slipMatch(
      Item line,
      List<Item> book,
      Set<Long> usedBook,
      int windowDays,
      Map<String, Set<String>> slips) {
    if (line.reference() == null || line.reference().isBlank()) {
      return Optional.empty();
    }
    Set<String> references = slips.getOrDefault(line.reference(), Set.of(line.reference()));
    List<Item> group =
        book.stream()
            .filter(b -> !usedBook.contains(b.id()))
            .filter(b -> b.reference() != null && references.contains(b.reference()))
            .filter(b -> b.amount().signum() == line.amount().signum())
            .filter(b -> days(b.date(), line.date()) <= windowDays)
            .toList();
    BigDecimal total = group.stream().map(Item::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (group.isEmpty() || total.compareTo(line.amount()) != 0) {
      return Optional.empty();
    }
    List<Long> ids = group.stream().map(Item::id).toList();
    usedBook.addAll(ids);
    return Optional.of(new Proposal(ids, List.of(line.id())));
  }

  private static Comparator<Item> preference(Item line) {
    return Comparator.comparing((Item b) -> referenceMatches(b, line) ? 0 : 1)
        .thenComparingLong(b -> days(b.date(), line.date()))
        .thenComparing(Item::id);
  }

  /**
   * Whether the texts of a book item and a bank line share a reference.
   *
   * @param b book item
   * @param line bank line
   * @return true when either reference is contained in the other side's text
   */
  static boolean referenceMatches(Item b, Item line) {
    String bookText = normalize(b.reference()) + " " + normalize(b.text());
    String bankText = normalize(line.reference()) + " " + normalize(line.text());
    return contains(bookText, normalize(line.reference()))
        || contains(bankText, normalize(b.reference()));
  }

  private static boolean contains(String text, String token) {
    return !token.isEmpty() && text.contains(token);
  }

  private static String normalize(String s) {
    return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
  }

  private static long days(LocalDate a, LocalDate b) {
    return Math.abs(ChronoUnit.DAYS.between(a, b));
  }

  /**
   * Item to match (book entry or bank line).
   *
   * @param id id
   * @param date date
   * @param amount signed amount (money in positive)
   * @param reference reference / cheque number
   * @param text narration or description
   */
  public record Item(Long id, LocalDate date, BigDecimal amount, String reference, String text) {}

  /**
   * Proposed match.
   *
   * @param bookIds book entry ids
   * @param bankIds bank line ids
   */
  public record Proposal(List<Long> bookIds, List<Long> bankIds) {}
}
