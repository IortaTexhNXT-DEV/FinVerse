import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { BulkUploadWizard } from '@/components/broking/BulkUploadWizard';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { UPLOADS } from './labels';
import type { UploadHandler } from './labels';
import './disbursement.css';

/**
 * Disbursement uploads (DIS 2.6.2, 2.8.1, 2.8.4, 3.26.2): payment requests, bank confirmations of
 * credits and negotiated checks, online banking approvals and the payee migration, each validated
 * row by row before the valid rows are committed.
 */
export default function UploadsPage() {
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const allowed = UPLOADS.filter((u) => can(u.permission));
  const [tab, setTab] = useState<UploadHandler>(allowed[0]?.id ?? 'DISB_REQUESTS');
  const current = allowed.find((u) => u.id === tab) ?? allowed[0];
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Disbursement"
        title="Disbursement Uploads"
        description="Upload payment requests, bank confirmations, online banking approvals and payees."
      />
      {current === undefined ? (
        <Card>You may not upload disbursement files.</Card>
      ) : (
        <>
          <Card flush>
            <div>
              <Tabs tabs={allowed} active={current.id} onChange={setTab} />
              <p className="dsb-intro">{current.description}</p>
            </div>
          </Card>
          <BulkUploadWizard
            key={current.id}
            handler={current.id}
            onCommitted={() => void queryClient.invalidateQueries({ queryKey: ['disbursement'] })}
          />
        </>
      )}
    </div>
  );
}
