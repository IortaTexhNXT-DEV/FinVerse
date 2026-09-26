import { DataTable } from '@/components/ui/DataTable';

/** An insurer line as typed on Record Claim. */
export interface LineDraft {
  insurerCode: string;
  sharePct: string;
  insurerClaimNo: string;
  reportedToInsurerOn: string;
  reserveAmount: string;
}

/**
 * The insurers of a new claim (BRCLM.043; FR-CL-021): proposed from the invoice shares of the
 * cover; the officer confirms the shares and enters the insurer claim numbers and reserves known
 * at recording.
 */
export function InsurerLinesEditor({
  lines,
  onChange,
}: Readonly<{ lines: LineDraft[]; onChange: (lines: LineDraft[]) => void }>) {
  const set = (index: number, key: keyof LineDraft, value: string) =>
    onChange(lines.map((l, i) => (i === index ? { ...l, [key]: value } : l)));
  const input = (index: number, key: keyof LineDraft, label: string, type = 'text') => (
    <input
      aria-label={`${label} of ${lines[index]?.insurerCode ?? ''}`}
      className="input"
      type={type}
      maxLength={type === 'text' ? 60 : undefined}
      min={type === 'number' ? 0 : undefined}
      step={type === 'number' ? 'any' : undefined}
      value={lines[index]?.[key] ?? ''}
      onChange={(e) => set(index, key, e.target.value)}
    />
  );
  return (
    <DataTable<LineDraft & { index: number }>
      caption="Insurers"
      rows={lines.map((l, index) => ({ ...l, index }))}
      rowKey={(l) => `${l.insurerCode}-${l.index}`}
      emptyMessage="The cover has no insurer shares; the lead insurer is used."
      columns={[
        { key: 'i', header: 'Insurer', render: (l) => l.insurerCode },
        {
          key: 's',
          header: 'Share %',
          render: (l) => input(l.index, 'sharePct', 'Share', 'number'),
        },
        {
          key: 'n',
          header: 'Insurer Claim No.',
          render: (l) => input(l.index, 'insurerClaimNo', 'Insurer claim number'),
        },
        {
          key: 'd',
          header: 'Reported to Insurer',
          render: (l) => input(l.index, 'reportedToInsurerOn', 'Date reported', 'date'),
        },
        {
          key: 'r',
          header: 'Insurer Reserve',
          render: (l) => input(l.index, 'reserveAmount', 'Reserve', 'number'),
        },
      ]}
    />
  );
}
