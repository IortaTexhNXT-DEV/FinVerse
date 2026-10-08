import type { BatchSendResult } from '@/api/quotations';

/** Splits a list of e-mail addresses typed by the user. */
export function emailList(text: string): string[] {
  return text
    .split(/[,;\s]+/)
    .map((a) => a.trim())
    .filter((a) => a.length > 0);
}

/** The outcome of a batch send: what was sent and what was not, with the reason. */
export function batchSendSummary(result: BatchSendResult): string {
  const sent = `${String(result.quotations)} quotation(s) sent in ${String(result.clients)} e-mail(s)`;
  const notSent = result.notSent ?? [];
  if (notSent.length === 0) {
    return sent;
  }
  const listed = notSent.map((n) => n.reference + ' (' + n.reason + ')').join(', ');
  return `${sent}; not sent: ${listed}`;
}
