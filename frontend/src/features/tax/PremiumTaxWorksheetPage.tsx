import { useState } from 'react';
import { Tabs } from '@/components/ui/Tabs';
import { WorksheetView } from './WorksheetView';

type Levy = 'PREMIUM_TAX' | 'LGT' | 'FST';

const TABS: readonly { id: Levy; label: string }[] = [
  { id: 'PREMIUM_TAX', label: 'Premium tax (2551Q)' },
  { id: 'LGT', label: 'Local government tax' },
  { id: 'FST', label: 'Fire service tax' },
];

const DESCRIPTIONS: Record<Levy, string> = {
  PREMIUM_TAX: 'Percentage tax on premiums of business not subject to VAT, filed quarterly.',
  LGT: 'Local business tax on premiums, paid quarterly to the LGU.',
  FST: 'Fire service tax on fire premiums, remitted monthly for the BFP.',
};

/** Premium tax, local government tax and fire service tax worksheets. */
export default function PremiumTaxWorksheetPage() {
  const [levy, setLevy] = useState<Levy>('PREMIUM_TAX');
  return (
    <div className="stack">
      <Tabs tabs={TABS} active={levy} onChange={setLevy} />
      <WorksheetView
        key={levy}
        kind={levy}
        title={TABS.find((t) => t.id === levy)?.label ?? levy}
        description={DESCRIPTIONS[levy]}
        granularity={levy === 'FST' ? 'MONTH' : 'QUARTER'}
      />
    </div>
  );
}
