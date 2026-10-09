import type { ReactNode } from 'react';
import { titleCase } from '@/utils/format';

interface KpiProps {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
  accent?: boolean;
}

/** Headline figure tile used on dashboards; the label in Title Case (acronyms kept). */
export function Kpi({ label, value, hint, accent = false }: Readonly<KpiProps>) {
  return (
    <div className={`card kpi ${accent ? 'accent' : ''}`}>
      <div className="kpi-label">{titleCase(label)}</div>
      <div className="kpi-value">{value}</div>
      {hint !== undefined && <div className="kpi-hint">{hint}</div>}
    </div>
  );
}
