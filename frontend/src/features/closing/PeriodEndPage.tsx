import { useState } from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { PeriodEndPanel } from './PeriodEndPanel';
import { usePeriodPicker } from './usePeriodPicker';
import { YearEndPanel } from './YearEndPanel';

type Tab = 'period' | 'year';

const TABS: readonly { id: Tab; label: string }[] = [
  { id: 'period', label: 'Period-end (monthly)' },
  { id: 'year', label: 'Year-end' },
];

/** Monthly and year-end closing checklists with the actions that complete them. */
export default function PeriodEndPage() {
  const picker = usePeriodPicker();
  const [tab, setTab] = useState<Tab>('period');
  return (
    <div className="stack">
      <PageHeader
        section="Planning & Closing"
        title="Period-End & Year-End"
        description="Automatic closing controls with pass/fail results. Monthly: soft close, revalue, close. Year-end: close income and expenses to retained earnings and open the next year."
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'period' ? <PeriodEndPanel picker={picker} /> : <YearEndPanel picker={picker} />}
    </div>
  );
}
