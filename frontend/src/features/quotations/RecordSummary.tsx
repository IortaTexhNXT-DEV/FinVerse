import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import { Card } from '@/components/ui/Card';
import '@/styles/quotation.css';

/** One fact of a record summary: icon, label and value. */
export interface Fact {
  icon: LucideIcon;
  label: string;
  value: ReactNode;
}

interface RecordSummaryProps {
  /** Name shown in bold (client or record name). */
  title: string;
  /** Codes, reference chips and status pills next to the name. */
  chips: ReactNode;
  facts: Fact[];
}

/**
 * Summary card of a record page (BDO Insure Record Details pattern): name, codes and status, then
 * key facts with their icons.
 */
export function RecordSummary({ title, chips, facts }: Readonly<RecordSummaryProps>) {
  return (
    <Card>
      <div className="record-summary">
        <div className="record-summary-head">
          <h2>{title}</h2>
          {chips}
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
