const UNITS: Readonly<Record<string, [string, string]>> = {
  Y: ['year', 'years'],
  M: ['month', 'months'],
  D: ['day', 'days'],
  H: ['hour', 'hours'],
  S: ['second', 'seconds'],
};

function part(amount: string, unit: string, minutes = false): string {
  const n = Number(amount);
  const [one, many] = minutes ? ['minute', 'minutes'] : (UNITS[unit] ?? [unit, unit]);
  return `${n} ${n === 1 ? one : many}`;
}

const DATE_PART = /^(?:(\d+)Y)?(?:(\d+)M)?(?:(\d+)D)?$/;
const TIME_PART = /^(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?$/;

/** The words of the matched groups, in order; nothing for a group that is absent. */
function words(match: RegExpExecArray | null, units: readonly string[]): string[] {
  if (match === null) {
    return [];
  }
  return units.flatMap((unit, i) => {
    const amount = match[i + 1];
    return amount === undefined ? [] : [part(amount, unit, unit === 'MI')];
  });
}

/**
 * An ISO-8601 duration or period in words: "PT15M" → "15 minutes", "P10Y" → "10 years",
 * "PT1H30M" → "1 hour 30 minutes". An unreadable value is returned as it is.
 */
export function durationText(iso: string): string {
  if (!iso.startsWith('P')) {
    return iso;
  }
  const [date, time] = iso.slice(1).split('T');
  const d = DATE_PART.exec(date ?? '');
  const t = time === undefined ? null : TIME_PART.exec(time);
  if (d === null || (time !== undefined && t === null)) {
    return iso;
  }
  const parts = [...words(d, ['Y', 'M', 'D']), ...words(t, ['H', 'MI', 'S'])];
  return parts.length === 0 ? iso : parts.join(' ');
}

/** The action of a legal hold request in words. */
export function holdActionLabel(action: 'PLACE' | 'RELEASE'): string {
  return action === 'PLACE' ? 'Place legal hold' : 'Release legal hold';
}

/** Percentage of the files copied, whole number; 100 when there is nothing to copy. */
export function copiedPercent(c: { total: number; moved: number }): number {
  return c.total === 0 ? 100 : Math.floor((c.moved * 100) / c.total);
}

/** Business names of the areas whose file content is copied into the file store. */
const CONTENT_AREAS: Readonly<Record<string, string>> = {
  doc_attachment_content: 'Document attachments',
  report_run_file: 'Archived report files',
  'report_run_file (STR files)': 'Suspicious transaction report files',
  report_batch: 'Report batches',
  msg_outbound_attachment: 'E-mail attachments',
  ops_extract_file: 'Operations extract files',
  csh_print_batch: 'Receipt print batches',
  rem_batch_document: 'Remittance batch documents',
  plc_slip_file: 'Placement slips',
  iss_upload_item: 'Policy upload files',
  iss_insurance_advice: 'Insurance advices',
  bkg_service_invoice: 'Service invoices',
  dsb_eod_output: 'Disbursement end-of-day files',
  clx_billing_document: 'Collection billing documents',
};

/** The business name of a content area, or the area as the server names it. */
export function contentAreaName(table: string): string {
  return CONTENT_AREAS[table] ?? table;
}

/** Where a cache keeps its entries: shared by every instance (Valkey) or in this instance only. */
export function cacheStoreLabel(store: string): string {
  return store === 'VALKEY' ? 'Shared (Valkey)' : 'This Instance';
}
