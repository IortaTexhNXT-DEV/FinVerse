import type { ReactNode } from 'react';

interface KpiProps {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
  accent?: boolean;
}

/** Headline figure tile used on dashboards. */
export function Kpi({ label, value, hint, accent = false }: Readonly<KpiProps>) {
  return (
    <div className={`card kpi ${accent ? 'accent' : ''}`}>
      <div className="kpi-label">{label}</div>
      <div className="kpi-value">{value}</div>
      {hint !== undefined && <div className="kpi-hint">{hint}</div>}
    </div>
  );
}
