import type { RowAction } from '@/components/ui/RowActions';
import { humanize } from '@/utils/format';

/**
 * The words of the Renewal screens for the codes the server returns (screen standards: lists show
 * labels, never codes) and the row action menus of the Renewal lists (one menu at the end of a
 * row, never buttons in the row).
 */

/** A label of a fixed list, the humanized code for a value the list does not know. */
function labelOf(
  labels: Readonly<Record<string, string>>,
  code: string | null | undefined,
): string {
  if (!code) {
    return '';
  }
  return labels[code] ?? humanize(code);
}

const RESPONSES: Readonly<Record<string, string>> = {
  RENEW_AS_IS: 'Renew as is',
  REVISE: 'Renew with revised terms',
  REJECT: 'Declined',
};

/** The insurer's response as the Record Insurer Response dialog names it. */
export function responseLabel(code: string | null | undefined): string {
  return labelOf(RESPONSES, code);
}

const LETTER_TYPES: Readonly<Record<string, string>> = {
  RA: 'Renewal Advice',
  NAL: 'No Advice Letter',
  NFR: 'Not for Renewal Letter',
  NRNS_REMINDER: 'NRNS Reminder',
  NON_ACCEPTANCE: 'Non-acceptance Letter',
};

/** The type of a renewal letter, with the notice of a Renewal Advice ("Renewal Advice, first notice"). */
export function letterTypeLabel(type: string, notice: string | null | undefined): string {
  const name = labelOf(LETTER_TYPES, type);
  return notice ? `${name}, ${notice.toLowerCase()} notice` : name;
}

const METHODS: Readonly<Record<string, string>> = {
  EMAIL: 'E-mail from the client',
  SIGNED_RA: 'Signed Renewal Advice',
  PAYMENT: 'Payment',
};

/** How the client accepted the renewal, as the Record Acceptance dialog names it. */
export function acceptanceMethodLabel(code: string | null | undefined): string {
  return labelOf(METHODS, code);
}

const SOURCES: Readonly<Record<string, string>> = {
  USER: 'User',
  SYSTEM_CHECK: 'System check',
  MATRIX: 'Decision matrix',
  UPLOAD: 'Upload',
  INSURER: 'Insurer response',
  LAMD: 'LAMD report',
};

/** Who gave a disposition (the user, a check, the decision matrix, an upload...). */
export function dispositionSourceLabel(code: string | null | undefined): string {
  return labelOf(SOURCES, code);
}

const OVERRIDES: Readonly<Record<string, string>> = {
  OUTSTANDING_BALANCE: 'Outstanding balance',
  CHECK: 'Failed check',
  BUCKET: 'Classification',
  DISPOSITION: 'Disposition',
  INSURER_MISMATCH: 'Insurer response mismatch',
  RA_UNLOCK: 'Unlock the Renewal Advice',
};

/** What an override changed, as the Override dialog names it. */
export function overrideKindLabel(code: string | null | undefined): string {
  return labelOf(OVERRIDES, code);
}

const BUCKETS: Readonly<Record<string, string>> = {
  CLEAN: 'Clean',
  REVIEW: 'Review',
  EXCEPTION: 'Exception',
};

const DISPOSITIONS: Readonly<Record<string, string>> = {
  FOR_RENEWAL: 'For Renewal',
  NOT_FOR_RENEWAL: 'Not for Renewal',
  FOR_QUOTATION: 'For Quotation',
  FOR_PROPOSAL: 'For Proposal',
  LOST_BUSINESS: 'Lost Business',
};

/** A value an override changed (a classification or a disposition), in words. */
export function overrideValueLabel(code: string | null | undefined): string {
  if (!code) {
    return '';
  }
  return BUCKETS[code] ?? DISPOSITIONS[code] ?? humanize(code);
}

/** "Review to Clean": the change an override made, in words; empty when it changed no value. */
export function overrideChange(from: string | null, to: string | null): string {
  if (!from && !to) {
    return '';
  }
  return `${overrideValueLabel(from) || 'None'} to ${overrideValueLabel(to) || 'None'}`;
}

const CAUSES: Readonly<Record<string, string>> = {
  RULE: 'Classification rules',
  OVERRIDE: 'Override',
};

/** What changed the classification of a renewal. */
export function bucketCauseLabel(code: string | null | undefined): string {
  return labelOf(CAUSES, code);
}

const CLOSED_AS: Readonly<Record<string, string>> = {
  RENEWED: 'Renewed',
  NOT_RENEWED: 'Not renewed',
  LOST: 'Lost business',
  EXPIRED_UNRENEWED: 'Expired without renewal',
  BOOKED_OTHER_INVOICE: 'Booked under another invoice',
};

/** How a renewal was closed. */
export function closedAsLabel(code: string | null | undefined): string {
  return labelOf(CLOSED_AS, code);
}

const MAP_SOURCES: Readonly<Record<string, string>> = {
  MIGRATION: 'Data migration',
  MANUAL: 'Entered on screen',
  UPLOAD: 'Upload',
};

/** Where a package mapping comes from. */
export function mapSourceLabel(code: string | null | undefined): string {
  return labelOf(MAP_SOURCES, code);
}

/** "Version 2" of a product (the product named by the caller). */
export function versionText(version: number | null | undefined): string {
  return version === null || version === undefined ? '' : `Version ${String(version)}`;
}

/** The row actions of a transfer request: Accept and Decline for the receiving unit, Cancel Request for the requester. */
export function transferActions(
  side: 'incoming' | 'outgoing',
  status: string,
  on: { accept: () => void; decline: () => void; cancel: () => void },
): RowAction[] {
  if (status !== 'REQUESTED') {
    return [];
  }
  if (side === 'incoming') {
    return [
      { label: 'Accept', onSelect: on.accept },
      { label: 'Decline', onSelect: on.decline, danger: true },
    ];
  }
  return [{ label: 'Cancel Request', onSelect: on.cancel, danger: true }];
}

/** The row actions of a set-up record with maker and checker: Authorize while it waits, Edit, Deactivate when active. */
export function setupRecordActions(
  recordStatus: string,
  may: boolean,
  what: string,
  on: { authorize: () => unknown; edit: () => void; deactivate?: () => unknown },
): RowAction[] {
  if (!may) {
    return [];
  }
  const actions: RowAction[] = [];
  if (recordStatus === 'PENDING_AUTHORIZATION') {
    actions.push({
      label: 'Authorize',
      onSelect: on.authorize,
      confirm: {
        title: `Authorize ${what}`,
        effect: `The ${what.toLowerCase()} change takes effect.`,
      },
    });
  }
  actions.push({ label: 'Edit', onSelect: on.edit });
  if (on.deactivate && recordStatus === 'ACTIVE') {
    actions.push({
      label: 'Deactivate',
      onSelect: on.deactivate,
      danger: true,
      confirm: {
        title: `Deactivate ${what}`,
        effect: `The ${what.toLowerCase()} is no longer used for new renewals.`,
        destructive: true,
      },
    });
  }
  return actions;
}

/** The row actions of a rule version: Submit a draft; Activate or Reject a submitted version. */
export function versionActions(
  status: string,
  versionNo: number,
  may: boolean,
  on: { submit: () => unknown; activate: () => unknown; reject: (reason: string) => unknown },
): RowAction[] {
  if (!may) {
    return [];
  }
  if (status === 'DRAFT') {
    return [{ label: 'Submit', onSelect: on.submit }];
  }
  if (status !== 'SUBMITTED') {
    return [];
  }
  return [
    {
      label: 'Activate',
      onSelect: on.activate,
      confirm: {
        title: `Activate Version ${String(versionNo)}`,
        effect: 'The version replaces the active one from its effective date.',
      },
    },
    {
      label: 'Reject',
      onSelect: on.reject,
      danger: true,
      confirm: {
        title: `Reject Version ${String(versionNo)}`,
        effect: 'The version is rejected and returns to its maker.',
        destructive: true,
        reason: 'required',
      },
    },
  ];
}

/** The row actions of a decision waiting for a second team member: Approve, Reject. */
export function decisionActions(on: { approve: () => void; reject: () => void }): RowAction[] {
  return [
    { label: 'Approve', onSelect: on.approve },
    { label: 'Reject', onSelect: on.reject, danger: true },
  ];
}
