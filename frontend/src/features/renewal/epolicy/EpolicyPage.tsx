import { Upload } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import { UploadPanel } from '../common/UploadPanel';
import { EpolicyReceiptsCard } from './EpolicyReceiptsCard';
import { EpolicySendingCard } from './EpolicySendingCard';
import '../renewal.css';

const TABS = [
  { id: 'receipts', label: 'E-Policy Receipts' },
  { id: 'sending', label: 'For E-Policy Sending' },
] as const;

type TabId = (typeof TABS)[number]['id'];

/**
 * E-Policies of the renewal accounts (FRRN.033): the e-policy files received from the insurers by
 * upload or MFT with their results, the sending of the e-policies to the clients through CCM, and
 * the upload of e-policy numbers.
 */
export default function EpolicyPage() {
  const { can } = useAuth();
  const [tab, setTab] = useState<TabId>('receipts');
  const [upload, setUpload] = useState(false);
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="E-Policies"
        description="E-policy files from the insurers and their sending to the clients."
        actions={
          can('RNW_PROCESS') && (
            <Button variant="secondary" icon={<Upload size={16} />} onClick={() => setUpload(true)}>
              Upload E-Policy Numbers
            </Button>
          )
        }
      />
      {upload && (
        <UploadPanel
          label="Upload E-Policy Numbers"
          handler="RNW_EPOLICY_NUMBERS"
          onClose={() => setUpload(false)}
        />
      )}
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'receipts' ? <EpolicyReceiptsCard /> : <EpolicySendingCard />}
    </div>
  );
}
