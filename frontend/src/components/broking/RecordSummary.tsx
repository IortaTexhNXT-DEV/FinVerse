import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import { Card } from '@/components/ui/Card';

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
 * Summary card of a record page (BDO Insure Record Details pattern): name, codes and status, then
 * key facts with their icons.
 */
export function RecordSummary({ title, chips, flags, facts }: Readonly<RecordSummaryProps>) {
  return (
    <Card>
      <div className="record-summary">
        <div className="record-summary-head">
          <h2>{title}</h2>
          {chips}
          {flags ? <span className="tag-list">{flags}</span> : null}
        </div>
        <div className="fact-grid">
          {facts.map(({ icon: Icon, label, value }) => (
            <div className="fact" key={label}>
              <Icon size={20} aria-hidden="true" />
              <span>
                <span className="fact-label">{label}</span>
                <span className="fact-value">{value ?? '—'}</span>
              </span>
            </div>
          ))}
        </div>
      </div>
    </Card>
  );
}
