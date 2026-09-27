import type { ReactNode } from 'react';
import { Card } from '@/components/ui/Card';
import type { Definition } from '@/components/ui/DefinitionGrid';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { isEmptyValue } from '@/utils/presentation';

/** A labelled status of the record (e.g. "KYC" with its own pill). */
export interface HeaderStatus {
  label: string;
  status: string;
  /** Pill text instead of the humanized status. */
  text?: string;
}

interface RecordHeaderProps {
  /** Record name (client, account, claim...); omit when the page title already shows it. */
  title?: string;
  /** Reference chips (codes with copy buttons). */
  chips?: ReactNode;
  /** The record status, then other labelled statuses (KYC, request state). */
  status?: string;
  statuses?: readonly HeaderStatus[];
  /** Record flags (FFY, Direct Payment, Information Incomplete) as tags. */
  flags?: ReactNode;
  /** Completeness of the record's required information. */
  completeness?: { filled: number; total: number };
  /** Key facts, four per row. */
  facts: readonly Definition[];
}

/** Completeness indicator: a bar and "8 of 12 fields". */
export function Completeness({ filled, total }: Readonly<{ filled: number; total: number }>) {
  const pct = total === 0 ? 100 : Math.round((filled / total) * 100);
  return (
    <span className="completeness" title={`${String(pct)}% of the required information`}>
      <span
        className={pct < 100 ? 'completeness-bar low' : 'completeness-bar'}
        role="meter"
        aria-label="Completeness"
        aria-valuenow={pct}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <span style={{ width: `${String(pct)}%` }} />
      </span>
      Profile {filled} of {total} Fields
    </span>
  );
}

/**
 * Summary header of a record page (BDO): the name, reference chips, status pills (the record's
 * status first, then labelled ones such as KYC), flag tags, a completeness indicator and a key
 * facts grid. The page actions stay in the page header, in the standard order.
 */
export function RecordHeader({
  title,
  chips,
  status,
  statuses = [],
  flags,
  completeness,
  facts,
}: Readonly<RecordHeaderProps>) {
  return (
    <Card className="record-header-card">
      <div className="record-header">
        <div className="record-header-top">
          {title !== undefined && <h2>{title}</h2>}
          {chips}
          {status !== undefined && <StatusBadge status={status} />}
          {statuses.map((s) => (
            <span key={s.label} className="labelled-status">
              <span className="muted">{s.label}</span>
              <StatusBadge status={s.status} label={s.text} />
            </span>
          ))}
          {flags ? <span className="tag-list">{flags}</span> : null}
          {completeness !== undefined && <Completeness {...completeness} />}
        </div>
        {facts.length > 0 && (
          <dl className="key-facts">
            {facts.map((f) => (
              <div key={f.label}>
                <dt>{f.label}</dt>
                <dd>{isEmptyValue(f.value) ? <span className="muted">—</span> : f.value}</dd>
              </div>
            ))}
          </dl>
        )}
      </div>
    </Card>
  );
}
