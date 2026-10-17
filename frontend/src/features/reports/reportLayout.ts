import type { MisField } from '@/api/misFields';
import type { ReportResult } from '@/api/reports';

/** The layout of a report (FR-CR-042): grouping fields and the chart of the PDF and Excel files. */
export interface ReportLayout {
  group1: string;
  group2: string;
  chart: 'NONE' | 'BAR' | 'LINE';
  /** An amount column the report totals, or COUNT (number of records). */
  chartValue: string;
}

/** The report as defined. */
export const NO_LAYOUT: ReportLayout = {
  group1: '',
  group2: '',
  chart: 'NONE',
  chartValue: 'COUNT',
};

const KEYS = {
  group1: 'layout.group1',
  group2: 'layout.group2',
  chart: 'layout.chart',
  chartValue: 'layout.chartValue',
} as const;

/** Whether the layout changes nothing. */
export function isPlain(layout: ReportLayout): boolean {
  return layout.group1 === '' && layout.group2 === '' && layout.chart === 'NONE';
}

/** The layout as parameters of the run (saved with a variant and a subscription). */
export function layoutParams(layout: ReportLayout): Record<string, string> {
  if (isPlain(layout)) {
    return {};
  }
  const out: Record<string, string> = {};
  if (layout.group1 !== '') {
    out[KEYS.group1] = layout.group1;
  }
  if (layout.group2 !== '' && layout.group2 !== layout.group1) {
    out[KEYS.group2] = layout.group2;
  }
  if (layout.chart !== 'NONE') {
    out[KEYS.chart] = layout.chart;
    out[KEYS.chartValue] = layout.chartValue;
  }
  return out;
}

/** The saved values of a variant split into the report parameters and the layout. */
export function splitLayout(saved: Record<string, string>): {
  values: Record<string, string>;
  layout: ReportLayout;
} {
  const values: Record<string, string> = {};
  Object.entries(saved).forEach(([k, v]) => {
    if (!k.startsWith('layout.')) {
      values[k] = v;
    }
  });
  const chart = saved[KEYS.chart];
  return {
    values,
    layout: {
      group1: saved[KEYS.group1] ?? '',
      group2: saved[KEYS.group2] ?? '',
      chart: chart === 'BAR' || chart === 'LINE' ? chart : 'NONE',
      chartValue: saved[KEYS.chartValue] ?? 'COUNT',
    },
  };
}

/** The MIS field that names a report column, when one is active. */
function activeField(key: string, fields: readonly MisField[]): MisField | undefined {
  return fields.find((f) => f.recordStatus === 'ACTIVE' && f.columnKeys.includes(key));
}

/**
 * The fields a result may be grouped by: its text and date columns, MIS fields first with their
 * catalogue label (one label across all reports).
 */
export function groupOptions(
  result: ReportResult,
  fields: readonly MisField[],
): { value: string; label: string }[] {
  const options = result.columns
    .filter((c) => c.type === 'TEXT' || c.type === 'DATE')
    .map((c) => {
      const mis = activeField(c.key, fields);
      return { value: c.key, label: mis ? `${mis.label} (MIS field)` : c.label, mis: !!mis };
    });
  return [...options.filter((o) => o.mis), ...options.filter((o) => !o.mis)].map(
    ({ value, label }) => ({ value, label }),
  );
}

/** What a chart may show: the number of records, or an amount the report totals. */
export function valueOptions(result: ReportResult): { value: string; label: string }[] {
  return [
    { value: 'COUNT', label: 'Number of records' },
    ...result.columns.filter((c) => c.summed).map((c) => ({ value: c.key, label: c.label })),
  ];
}

/**
 * Warnings for a layout that groups by a field whose MIS field has been deactivated since the
 * variant was saved (FR-CR-062): the report still groups by the column, under its own label.
 */
export function inactiveWarnings(layout: ReportLayout, fields: readonly MisField[]): string[] {
  return [layout.group1, layout.group2]
    .filter((key) => key !== '')
    .flatMap((key) => {
      const named = fields.filter((f) => f.columnKeys.includes(key));
      return named.length > 0 && named.every((f) => f.recordStatus === 'INACTIVE')
        ? [`The MIS field ${named[0]?.label ?? key} used by this layout is no longer active.`]
        : [];
    });
}
