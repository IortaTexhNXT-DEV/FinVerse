import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { misFieldsApi } from '@/api/misFields';
import type { ReportResult } from '@/api/reports';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Notice } from '@/components/ui/Notice';
import { SelectInput } from '@/features/assets/FormControls';
import type { ReportLayout } from './reportLayout';
import { NO_LAYOUT, groupOptions, inactiveWarnings, isPlain, valueOptions } from './reportLayout';

const CHARTS = [
  { value: 'NONE', label: 'No chart' },
  { value: 'BAR', label: 'Bar chart' },
  { value: 'LINE', label: 'Line chart' },
];

/**
 * The Layout panel of the report runner (FR-CR-042): one or two grouping fields (the MIS fields of
 * FR-CR-062 first, with their catalogue label) with subtotals and counts per group, and one bar or
 * line chart of a grouped amount or count in the PDF and Excel files. The layout is saved with a
 * variant; it changes the presentation only.
 */
export function LayoutPanel({
  result,
  layout,
  busy,
  onApply,
}: Readonly<{
  result: ReportResult;
  layout: ReportLayout;
  busy: boolean;
  onApply: (layout: ReportLayout) => void;
}>) {
  const [draft, setDraft] = useState<ReportLayout>(layout);
  const fields = useQuery({ queryKey: ['mis-fields'], queryFn: misFieldsApi.list });
  const groups = groupOptions(result, fields.data ?? []);
  const values = valueOptions(result);
  const set = (patch: Partial<ReportLayout>) => setDraft((d) => ({ ...d, ...patch }));
  const chartWithoutGroup = draft.chart !== 'NONE' && draft.group1 === '';
  const warnings = inactiveWarnings(layout, fields.data ?? []);
  return (
    <Card title="Layout">
      <div className="stack">
        {warnings.length > 0 && <Notice tone="warning" items={warnings} />}
        <div className="form-grid">
          <SelectInput
            label="Group by"
            value={draft.group1}
            blank="No grouping"
            options={groups}
            onChange={(group1) => set({ group1, group2: group1 === '' ? '' : draft.group2 })}
          />
          <SelectInput
            label="Then by"
            value={draft.group2}
            blank="No second grouping"
            disabled={draft.group1 === ''}
            options={groups.filter((g) => g.value !== draft.group1)}
            onChange={(group2) => set({ group2 })}
          />
          <SelectInput
            label="Chart"
            value={draft.chart}
            options={CHARTS}
            error={chartWithoutGroup ? 'Select a grouping field for the chart' : undefined}
            onChange={(chart) =>
              set({ chart: chart === 'BAR' || chart === 'LINE' ? chart : 'NONE' })
            }
          />
          <SelectInput
            label="Chart value"
            value={draft.chartValue}
            disabled={draft.chart === 'NONE'}
            options={values}
            onChange={(chartValue) => set({ chartValue })}
          />
        </div>
        <p className="muted">
          Subtotals and the number of records show for each group; the chart is drawn in the PDF and
          Excel files. Save a variant to keep the layout.
        </p>
        <div className="row">
          <Button busy={busy} disabled={chartWithoutGroup} onClick={() => onApply(draft)}>
            Apply Layout
          </Button>
          {!isPlain(layout) && (
            <Button
              variant="ghost"
              onClick={() => {
                setDraft(NO_LAYOUT);
                onApply(NO_LAYOUT);
              }}
            >
              Clear Layout
            </Button>
          )}
        </div>
      </div>
    </Card>
  );
}
