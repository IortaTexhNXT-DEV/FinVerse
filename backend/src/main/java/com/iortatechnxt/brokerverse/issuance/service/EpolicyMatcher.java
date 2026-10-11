package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.issuance.domain.MatchMethod;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyService.ReceivedFile;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Finds the account of a received e-policy (BRNB.073) among the live accounts not yet booked: the
 * ARN chosen by the user, else the policy number given, else the only ARN in the file name (naming
 * convention {@code ARN-yyyy-nnnnnn_...pdf}), else the only ARN printed in the document. It never
 * throws, so a bulk upload can propose a match for every file.
 */
@Component
public class EpolicyMatcher {

  private final AccountQueryService accounts;
  private final PolicyDataExtractor extractor;

  /**
   * Creates the matcher.
   *
   * @param accounts account reads
   * @param extractor policy data extraction (ARNs printed in the document)
   */
  public EpolicyMatcher(AccountQueryService accounts, PolicyDataExtractor extractor) {
    this.accounts = accounts;
    this.extractor = extractor;
  }

  /**
   * The account of a file.
   *
   * @param file file and hints
   * @return account and how it was found; empty when none or several match
   */
  public Optional<Resolution> match(ReceivedFile file) {
    if (!blank(file.arn())) {
      return byArn(file.companyId(), file.arn().strip(), MatchMethod.MANUAL);
    }
    if (!blank(file.policyNo())) {
      List<Account> found = accounts.preBooked(file.companyId(), file.policyNo().strip());
      if (found.size() == 1) {
        return Optional.of(new Resolution(found.get(0), MatchMethod.POLICY_NUMBER));
      }
    }
    return only(file.companyId(), PolicyTextParser.arns(file.fileName()), MatchMethod.FILE_NAME)
        .or(
            () ->
                only(
                    file.companyId(),
                    extractor.extract(file.content(), null).arns(),
                    MatchMethod.CONTENT));
  }

  private Optional<Resolution> only(Long companyId, List<String> arns, MatchMethod method) {
    return arns.size() == 1 ? byArn(companyId, arns.get(0), method) : Optional.empty();
  }

  private Optional<Resolution> byArn(Long companyId, String arn, MatchMethod method) {
    return accounts.preBooked(companyId, arn).stream()
        .filter(a -> a.getArn().equals(arn))
        .findFirst()
        .map(a -> new Resolution(a, method));
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  /**
   * The account of a file and how it was found.
   *
   * @param account account
   * @param method match method
   */
  public record Resolution(Account account, MatchMethod method) {}
}
