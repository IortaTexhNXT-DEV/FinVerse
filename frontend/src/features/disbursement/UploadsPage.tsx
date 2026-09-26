import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { BulkUploadWizard } from '@/components/broking/BulkUploadWizard';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import './disbursement.css';

type Handler =
  | 'DISB_REQUESTS'
  | 'DISB_CHECKS_NEGOTIATED'
  | 'DISB_CTA_CREDITED'
  | 'DISB_BOB_APPROVED'
  | 'DISB_PAYEE_MIGRATION';

const UPLOADS: readonly { id: Handler; label: string; text: string; permission: string }[] = [
  {
    id: 'DISB_REQUESTS',
    label: 'Payment Requests',
    text: 'Payment requests of other units (DIS 2.6.2). Rows with a maintained payee become vouchers at once; the others wait for the payee.',
    permission: 'DISB_UPLOAD',
  },
  {
    id: 'DISB_CHECKS_NEGOTIATED',
    label: 'Negotiated Checks',
    text: 'The checks deposited by payees from the bank statement (DIS 3.26.2); each check becomes Negotiated and its clearing entry posts.',
    permission: 'DISB_UPLOAD',
  },
  {
    id: 'DISB_CTA_CREDITED',
    label: 'Credited Accounts',
    text: 'The credit-to-account confirmation of the bank (DIS 2.8.1); each credit becomes Credited.',
    permission: 'DISB_UPLOAD',
  },
  {
    id: 'DISB_BOB_APPROVED',
    label: 'BOB Approvals',
    text: 'The online banking approvals exported from BOB (DIS 2.8.4); each payment becomes Debited.',
    permission: 'DISB_UPLOAD',
  },
  {
    id: 'DISB_PAYEE_MIGRATION',
    label: 'Payee Migration',
    text: 'Payees and their bank accounts from the former system (DIS 2.2.0); they come in for authorisation.',
    permission: 'DISB_PAYEE_MAINTAIN',
  },
];

/**
 * Disbursement uploads (DIS 2.6.2, 2.8.1, 2.8.4, 3.26.2): payment requests, bank confirmations of
 * credits and negotiated checks, online banking approvals and the payee migration, each validated
 * row by row before the valid rows are committed.
 */
export default function UploadsPage() {
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const allowed = UPLOADS.filter((u) => can(u.permission));
  const [tab, setTab] = useState<Handler>(allowed[0]?.id ?? 'DISB_REQUESTS');
  const current = allowed.find((u) => u.id === tab) ?? allowed[0];
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Disbursement"
        title="Disbursement Uploads"
        description="Upload payment requests, bank confirmations, online banking approvals and payees; valid rows are committed after review."
      />
      {current === undefined ? (
        <Card>You may not upload disbursement files.</Card>
      ) : (
        <>
          <Card flush>
            <div>
              <Tabs tabs={allowed} active={current.id} onChange={setTab} />
              <p className="dsb-intro">{current.text}</p>
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
