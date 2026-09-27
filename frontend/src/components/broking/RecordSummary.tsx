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
        <dl className="key-facts">
          {facts.map(({ icon: Icon, label, value }) => (
            <div className="key-fact" key={label}>
              <dt>
                <Icon size={14} aria-hidden="true" />
                {label}
              </dt>
              <dd>
                {isEmptyValue(value) || value === '—' ? <span className="muted">—</span> : value}
              </dd>
            </div>
          ))}
        </dl>
      </div>
    </Card>
  );
}
