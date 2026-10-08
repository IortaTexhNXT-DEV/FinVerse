import { formatAmount, formatCompact } from '@/utils/format';

interface FigureProps {
  label: string;
  value: number;
  hint?: string;
  danger?: boolean;
}

/** One labelled amount of a widget (compact value, exact amount on hover). */
export function Figure({ label, value, hint, danger = false }: Readonly<FigureProps>) {
  return (
    <div>
      <div className="kpi-label">{label}</div>
      <div className={`widget-figure-value ${danger ? 'danger' : ''}`} title={formatAmount(value)}>
        {formatCompact(value)}
      </div>
      {hint !== undefined && <div className="kpi-hint">{hint}</div>}
    </div>
  );
}
