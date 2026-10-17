import { useBaseCurrency } from '@/context/workspaceContext';
import { inCurrency } from '@/utils/currencyLabel';

/** A column heading or label in the base currency of the selected company. */
export function InBaseCurrency({ label }: Readonly<{ label: string }>) {
  const currency = useBaseCurrency();
  return <>{inCurrency(label, currency)}</>;
}
