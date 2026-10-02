import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import { Card } from '@/components/ui/Card';
import { isEmptyValue } from '@/utils/presentation';

/** One fact of a record summary: icon, label and value. */
export interface Fact {
  icon: LucideIcon;
  label: string;
  value: ReactNode;
}

interface RecordSummaryProps {
  /** Name shown in bold (client or record name). */
  title: string;
  /** Codes, reference chips and the status pill next to the name. */
  chips: ReactNode;
  /** Record flags (FFY, Direct Payment ...) shown as small chips, apart from the status pill. */
  flags?: ReactNode;
  facts: Fact[];
}

/**
 * Key facts in the record header style: icon and label over the value, four per row; an empty
 * fact shows a muted dash. Used by the summary card and by cards that show a record's outcome in
 * the same style (e.g. the validation of a package version).
 */
export function KeyFacts({ facts, label }: Readonly<{ facts: Fact[]; label?: string }>) {
  return (
    <dl className="key-facts" aria-label={label}>
      {facts.map(({ icon: Icon, label: name, value }) => (
        <div className="key-fact" key={name}>
          <dt>
            <Icon size={14} aria-hidden="true" />
            {name}
          </dt>
          <dd>{isEmptyValue(value) || value === '—' ? <span className="muted">—</span> : value}</dd>
        </div>
      ))}
    </dl>
  );
}

/**
 * Summary card of a record page (BDO Insure Record Details pattern, same layout as
 * `RecordHeader`): name, codes and status, flags, then the key facts in four columns with their
 * icons; an empty fact shows a muted dash.
 */
export function RecordSummary({ title, chips, flags, facts }: Readonly<RecordSummaryProps>) {
  return (
    <Card className="record-header-card">
      <div className="record-header">
        <div className="record-header-top">
          <h2>{title}</h2>
          {chips}
          {flags ? <span className="tag-list">{flags}</span> : null}
        </div>
        <KeyFacts facts={facts} />
      </div>
    </Card>
  );
}
