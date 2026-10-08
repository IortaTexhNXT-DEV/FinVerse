import { useQuery } from '@tanstack/react-query';
import type { KpiBreakdownItem } from '@/components/ui/kpiBreakdown';
import { useCompanyId, useWorkspace } from '@/context/workspaceContext';
import { formatAmount, humanize } from '@/utils/format';
import { monthLabel } from '../dashboardMath';

/** Chart colours: the brand tokens of styles/tokens.css (no ad-hoc colours). */
export const CHART = {
  primary: 'var(--brand-blue)',
  secondary: 'var(--brand-gold)',
  accent: 'var(--brand-navy)',
  muted: 'var(--color-border-strong)',
  grid: 'var(--color-border)',
} as const;

/** Height of the widget charts in pixels. */
export const CHART_HEIGHT = 220;

/**
 * Loads one dashboard widget for the selected company and branch. Each widget has its own query,
 * so a slow or failing widget never blocks the others.
 */
export function useWidget<T>(
  name: string,
  load: (companyId: number, branchId?: number) => Promise<T>,
  enabled: boolean,
) {
  const companyId = useCompanyId();
  const { branchId } = useWorkspace();
  return useQuery({
    queryKey: ['dashboard-widget', name, companyId, branchId],
    queryFn: () => load(companyId, branchId),
    enabled: enabled && companyId > 0,
  });
}

/** Chart tooltip value formatter: amounts in accounting style. */
export function tooltipAmount(value: unknown): string {
  return formatAmount(typeof value === 'number' ? value : Number(value));
}

/** Chart tooltip label formatter for `yyyy-MM` categories. */
export function tooltipMonth(label: unknown): string {
  return typeof label === 'string' ? monthLabel(label) : '';
}

/** The approvals waiting per module, as ranked breakdown lines of the tile (never a dotted line). */
export function approvalLines(byModule: Record<string, number>): KpiBreakdownItem[] {
  return Object.entries(byModule).map(([module, count]) => ({
    key: module,
    label: humanize(module),
    count,
    to: '/approvals',
  }));
}
