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
      ],
    },
  ],
};
