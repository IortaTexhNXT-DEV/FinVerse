import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Sanction Screening screens (BRD-10, SNSRP-301-903), in sidebar order. */
export const SCREENING_HELP: HelpSection = {
  id: 'screening',
  module: 'Sanction Screening',
  intro:
    'Sanction Screening checks clients against the sanctions, PEP and internal watchlists when they are registered, when an account is submitted, when a list changes and in the nightly run. Matches become screening cases that are investigated, approved by the unit head, reviewed by Compliance and, when needed, decided by the AML Committee and reported to the AMLC as a suspicious transaction report (STR). Screening informs; it does not block business unless SCR_BLOCK_ON_OPEN_MATCH is switched on.',
  screens: [
    {
      name: 'Screening Home',
      path: '/screening',
      summary:
        'Your screening work: open cases by stage, cases due today or past their SLA, potential matches not yet cased and the status of the last watchlist run.',
      workflow: [
        'A case moves New -> Investigation -> Unit head approval -> Compliance review, then to the AML Committee, STR preparation and extraction, or is closed (workflow SCR_CASE).',
        'A disapproved or returned case goes back to the investigator with the reason.',
      ],
      controls: [
        'You see screening work only with SCR_VIEW; each stage needs its own permission (SCR_INVESTIGATE, SCR_CASE_APPROVE, SCR_COMPLIANCE_REVIEW, SCR_COMMITTEE, SCR_STR_EXTRACT).',
        'The risk rating and the PEP / WATCHLIST_REVIEW tags of a client are set only through screening and are kept in the client history and the audit trail (SNSRP-302, 304).',
      ],
    },
    {
      name: 'Cases',
      path: '/screening/cases',
      summary:
        'The screening cases by tab (My Cases, Team, For Approval, Committee, STR, Closed, All) with the stage, assignee and SLA badge; open a case for its matches, review form, documents, decisions, STR and timeline.',
      workflow: [
        'A case is opened by itself when screening records a match at or above the case threshold, when a risk rule sets a category that needs a case, or when an account is submitted; a new match joins the open case of the same client and type.',
        'The investigator (assigned by the assignment matrix) completes the review form, uploads the KYC documents and submits with a disposition; Need More Info sends it back to the account officer.',
        'The unit head approves or disapproves with a reason; Compliance closes it, returns it to the investigator or refers it to the AML Committee, which decides by majority (rule SCR_COMMITTEE_RULE, size SCR_COMMITTEE_SIZE); a decision to report opens the STR stage.',
        'The job SCR_SLA_MONITOR sends a reminder before the due time of the stage and, when it passes, raises SCR_SLA_BREACH and escalates to the role of the SLA matrix.',
        'Re-assign moves a case to another eligible user with a reason (SCR_REASSIGN_REASON); Re-open brings a closed case back to Compliance review with a reason.',
      ],
      controls: [
        'Search needs at least 3 characters; the tab and filters stay in the address so a list can be shared.',
        'A submission is refused while the mandatory review fields or the documents required by the validation rules are missing.',
        'The investigator of a case can never approve it at a later stage, and a committee member votes once per round (four-eyes).',
        'Every step, reason and vote is kept in the case timeline, which cannot be changed.',
      ],
    },
    {
      name: 'Matches',
      path: '/screening/matches',
      summary:
        'Clients matched against the sanctions, PEP and internal lists, with the score, the algorithm (exact, phonetic or fuzzy) and the fields that matched. Open a match to compare the client and the list entry side by side.',
      workflow: [
        'Screening runs by itself when a client is registered or its name, birth date, nationality, TIN or ID changes, when an account is submitted, when a list entry is approved and in the nightly batch (full list once a month).',
        'The risk rules then tag the client: the rating and the PEP / WATCHLIST_REVIEW tags of the qualifying category are set in the client master and the KYC review comes forward when the rating goes up.',
        "Open Case starts a screening case for the match, or adds it to the client's open case of the same type.",
        'Mark False Positive clears the match with a justification and at least one evidence document attached to the match; with SCR_RISK_TAG the rating can be corrected and the Watchlist Review tag ended at the same time.',
      ],
      controls: [
        'The same client, entry version and configuration version never produce a second match.',
        'A false positive is not raised again for the same entry version; when the entry changes it is matched again (SNSRP-304, SQ12).',
        'A rule never lowers a rating; only a manual change with evidence does.',
        'Deciding matches needs SCR_INVESTIGATE; every decision is kept in the audit trail.',
      ],
    },
    {
      name: 'Screening Runs',
      path: '/screening/runs',
      summary:
        'The log of every screening run: trigger and reference, clients in scope, whether the whole list or only the changed entries were screened, the configuration versions used and the counts.',
      workflow: [
        'A run is logged for each registered or changed client, each submitted account, each list change and each batch window (job SCR_PERIODIC_SCREENING, 01:30).',
        'Open a run to see the matches it recorded.',
      ],
      controls: [
        'The batch screens only the client statuses of parameter SCR_SCREENING_SCOPE (prospects and confirmed clients); inactive clients are never screened.',
        'Without active matching criteria no run starts and the alert SCR_NO_ACTIVE_CONFIG is raised.',
      ],
    },
    {
      name: 'High-risk Clients',
      path: '/screening/high-risk',
      summary:
        'The clients rated high risk or tagged PEP or Watchlist Review, with their risk category, when and how they were tagged, their open screening case, active policy and marketing unit.',
      workflow: [
        'Filter by risk category, marketing unit or client type.',
        'Export the list to Excel or PDF (report SCR-HIGH-RISK-CLIENTS) with SCR_REPORT_VIEW.',
      ],
      controls: [
        'The list reads the risk profile set by screening; ratings and tags are changed only on a match or a case, with a justification.',
      ],
    },
    {
      name: 'STR',
      path: '/screening/str',
      summary:
        'The register of suspicious transaction reports by status (Draft, For Approval, Approved, Extracted, Filed), the extraction batches and their files.',
      workflow: [
        'Compliance prepares the STR on the STR tab of the case: the reason codes, the template fields and the transactions (prefilled from the client accounts), then marks it ready.',
        'Extract Approved STRs lists the AML Committee-approved STRs of a period and writes them to one file in the AMLC layout (SCR_STR_EXTRACT); the file is archived with its SHA-256.',
        'After filing on the AMLC portal, Record Filing on the case stores the AMLC reference and filing date; the STR becomes Filed and the case closes.',
      ],
      controls: [
        'Only committee-approved STRs are extracted; extracting one again needs a reason and is logged as a re-extraction.',
        'The AMLC reference is unique and the filing date cannot be before the extraction or in the future.',
        'The STR reason list (SCR_STR_REASON) and the AMLC layout are to be supplied by BDOI (SQ09); SIT/UAT uses placeholder reasons.',
      ],
    },
  ],
};

/** In-app help of the Compliance Setup screens (BRD-10, SNSRP-101-109, 201-204). */
export const SCREENING_SETUP_HELP: HelpSection = {
  id: 'screening-setup',
  module: 'Compliance Setup',
  intro:
    'Compliance Setup holds everything screening runs on as dated versions under maker-checker: matching criteria, risk categories and rules, approval, assignment and SLA matrices, validation rules, review and STR templates, and the watchlists with their sources.',
  screens: [
    {
      name: 'Configuration Versions',
      path: '/screening-setup/config',
      summary:
        'One tab per configuration type with its active version, drafts and history; a draft is edited, compared with the active version and submitted for approval.',
      workflow: [
        'A Compliance Officer (SCR_CONFIG_MAINTAIN) drafts a change as a new version and submits it with an effective date.',
        'A Compliance Checker (SCR_CONFIG_APPROVE) approves or rejects it from My Approvals; the approved version becomes active on its date.',
      ],
      controls: [
        'An active version never changes; a change is always a new version, and a case keeps the version it started with (SNSRP-104).',
        'The maker of a version can never approve it (SNSRP-109).',
        'Without an active matching version a screening trigger raises SCR_NO_ACTIVE_CONFIG.',
        'Only one draft or pending version exists per type; New Draft opens it. The effective date is today or later, and a draft that changes nothing cannot be submitted.',
        'Changes lists every rule and attribute with its value before and after, against the version in force; the list is kept with the version when it is submitted.',
      ],
    },
    {
      name: 'Templates',
      path: '/screening-setup/templates',
      summary:
        'The KYC review, transaction review, EDD and STR templates: sections, fields, data types, lists of values, mandatory flags and help texts, with a preview of the form.',
      workflow: [
        'Open the template type, click New Draft, add or change fields, check the preview and submit the version for approval.',
        'A Compliance Checker approves it; reviews and STRs started after its effective date use it, those already started keep their version (SNSRP-104, 105).',
      ],
      controls: [
        'Every field needs a label and a code of capitals, digits and underscores; a list field needs its list of values.',
        'A template needs at least one mandatory field. The AMLC STR format is parked until BDOI supplies it (SQ09).',
      ],
    },
    {
      name: 'Watchlist',
      path: '/screening-setup/watchlist',
      summary:
        'The sanctioned names, PEPs and internal watchlist entries by source and status, with their aliases and change history.',
      workflow: [
        'A Compliance Officer adds an entry, changes it or deactivates it with a reason; the change waits in Pending Changes (SNSRP-203).',
        'A Compliance Checker compares the before and after values and approves or rejects with remarks; an approved entry is active from today and screened (SNSRP-204).',
      ],
      controls: [
        'Screening uses active entries only; pending and draft entries have no effect.',
        'Entries are never deleted, only deactivated; one change per entry waits at a time.',
        'The maker of a change can never approve it. The SIT/UAT watchlist uses invented names only.',
      ],
    },
    {
      name: 'List Sources and Runs',
      path: '/screening-setup/sources',
      summary:
        'The list sources (AML advisory, NLDS-PEP, internal), the list file template, Upload List File and the log of every ingestion run with its failed records.',
      workflow: [
        'The job SCR_WATCHLIST_INGEST (01:00) reads the file staged on each file source and applies the official list at once; a source without a new file gets a FAILED run.',
        'Upload List File logs a run at once; its additions, updates and delistings wait for a Compliance Checker, who can approve them all from the run.',
        'SCR_INGEST_ERROR_DIGEST (07:00, Monday to Friday) e-mails the failed records to SCR_INGEST_ALERT_RECIPIENTS (SNSRP-202).',
      ],
      controls: [
        'A file is CSV or XLSX in the template; each failed line is kept with its reason and a FAILED or PARTIAL run raises SCR_INGEST_FAILED.',
        'A reference already on the list is updated, never duplicated; on a full-file source the entries missing from the file are delisted.',
        'API and NLDS transports are parked until BDOI names them (SQ01).',
      ],
    },
  ],
};
