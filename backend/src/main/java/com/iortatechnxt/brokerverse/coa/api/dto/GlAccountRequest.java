package com.iortatechnxt.brokerverse.coa.api.dto;

import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.coa.domain.AccountLevel;
import com.iortatechnxt.brokerverse.coa.domain.SubLedgerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

/**
 * Create / update GL account request (General Ledger Heads / Sub GL / Micro GL maintenance).
 *
 * <p>{@code companyId}, {@code code}, {@code accountClass}, {@code level} and {@code openedOn} are
 * immutable after creation and ignored on update.
 *
 * @param companyId company
 * @param code account code
 * @param name account name
 * @param shortName concise description
 * @param accountClass classification
 * @param level tier
 * @param parentCode parent account code (null for top-level group)
 * @param categoryCode GL category code
 * @param controlAccount control account flag
 * @param subLedgerType controlled sub-ledger
 * @param allowManualPosting manual journals allowed
 * @param costCenterRequired cost centre mandatory at posting
 * @param businessLineRequired line of business mandatory at posting
 * @param revaluationRequired foreign currency balances revalued at period end
 * @param reconcilable nominal / reconcilable account
 * @param interBranch inter-branch account
 * @param contraAccountCode contra account
 * @param reportGroup financial statement line
 * @param openedOn date of opening
 * @param allowedCurrencies allowed currencies (empty = all)
 * @param allowedBranchIds allowed posting branches (empty = all)
 * @param allowedRoleCodes access codes (empty = all roles)
 */
public record GlAccountRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 30) @Pattern(regexp = "[0-9A-Z.\\-]+") String code,
    @NotBlank @Size(max = 150) String name,
    @Size(max = 40) String shortName,
    @NotNull AccountClass accountClass,
    @NotNull AccountLevel level,
    @Size(max = 30) String parentCode,
    @Size(max = 10) String categoryCode,
    boolean controlAccount,
    SubLedgerType subLedgerType,
    boolean allowManualPosting,
    boolean costCenterRequired,
    boolean businessLineRequired,
    boolean revaluationRequired,
    boolean reconcilable,
    boolean interBranch,
    @Size(max = 30) String contraAccountCode,
    @Size(max = 60) String reportGroup,
    @NotNull LocalDate openedOn,
    Set<@Pattern(regexp = "[A-Z]{3}") String> allowedCurrencies,
    Set<Long> allowedBranchIds,
    Set<String> allowedRoleCodes) {}
