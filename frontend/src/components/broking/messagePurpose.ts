import { humanize } from '@/utils/format';

/** What an e-mail was sent for, by name: the purpose codes of the send log are never shown. */
const PURPOSE_LABELS: Readonly<Record<string, string>> = {
  CLX_SOA: 'Statement of Account',
  EB_SOA: 'Statement of Account',
  PASSWORD: 'Password',
  EPOLICY: 'E-policy',
  BCL_LOSS_ADVICE: 'Loss Advice',
  CSF_RESEND_RA: 'Renewal Advice (resent)',
  EB_RFP: 'Request for Proposal',
  PKG_QUOTATION_SLIP: 'Quotation Slip',
  PKG_ADVISORY: 'Advisory',
  PRODRECON: 'Production Reconciliation',
  SCR_INGEST_DIGEST: 'Screening List Update',
};

/** The purpose of a sent e-mail as the send log shows it. */
export function purposeLabel(code: string): string {
  return PURPOSE_LABELS[code] ?? humanize(code);
}
