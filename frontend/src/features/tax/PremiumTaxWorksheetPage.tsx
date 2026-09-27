import { useSearchParams } from 'react-router-dom';
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

/** Premium tax, local government tax and fire service tax worksheets (a new levy starts on its default period). */
export default function PremiumTaxWorksheetPage() {
  // The levy is in the URL (?levy=FST) like the period, so Back from a drill-down returns to it.
  const [params, setParams] = useSearchParams();
  const levy = TABS.find((t) => t.id === params.get('levy'))?.id ?? 'PREMIUM_TAX';
  const setLevy = (next: Levy) => setParams({ levy: next }, { replace: true });
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
