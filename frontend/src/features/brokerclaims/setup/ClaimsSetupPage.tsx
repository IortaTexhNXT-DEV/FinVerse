import { useState } from 'react';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { CLAIMS_SECTION } from '../ClaimsPlaceholder';
import { SETTLEMENT_LIST, STATUS_LIST } from './api';
import { AttributesTab } from './AttributesTab';
import { HandlersTab } from './HandlersTab';
import { ListsTab } from './ListsTab';
import { MatrixTab } from './MatrixTab';

type SetupTab = 'statuses' | 'settlements' | 'matrix' | 'handlers' | 'lists';

const TABS: readonly { id: SetupTab; label: string }[] = [
  { id: 'statuses', label: 'Status Attributes' },
  { id: 'settlements', label: 'Settlement Types' },
  { id: 'matrix', label: 'Status Access Matrix' },
  { id: 'handlers', label: 'Claims Handler Register' },
  { id: 'lists', label: 'Claims Lists' },
];

function TabContent({ tab }: Readonly<{ tab: SetupTab }>) {
  switch (tab) {
    case 'statuses':
      return <AttributesTab list={STATUS_LIST} />;
    case 'settlements':
      return <AttributesTab list={SETTLEMENT_LIST} />;
    case 'matrix':
      return <MatrixTab />;
    case 'handlers':
      return <HandlersTab />;
    default:
      return <ListsTab />;
  }
}

/**
 * Claims Setup (BRCLM.010/012/014/017/036; FR-CM-040/041/043): status and settlement type
 * attributes, the status access matrix, the claims handler register and the Claims lists, maintained
 * by the Unit Head (BCL_SETUP) and authorized by another user.
 */
export default function ClaimsSetupPage() {
  const [tab, setTab] = useState<SetupTab>('statuses');
  return (
    <div className="stack">
      <PageHeader
        section={CLAIMS_SECTION}
        title="Claims Setup"
        description="Status and settlement type attributes, the status access matrix, the claims handler register and the Claims lists; every change waits for another user's authorization."
      />
      <Card flush>
        <Tabs tabs={TABS} active={tab} onChange={setTab} />
        <TabContent tab={tab} />
      </Card>
    </div>
  );
}
