import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { BulkUploadWizard } from '@/components/broking/BulkUploadWizard';
import { PageHeader } from '@/components/ui/PageHeader';

/**
 * Bulk acceptance of quotations (BRNB.024/044): the ARNs of quotations accepted by clients, with
 * the accepted risk groups; optionally the accounts are created at once for confirmed clients.
 */
export default function AcceptanceBulkPage() {
  const queryClient = useQueryClient();
  const [createAccounts, setCreateAccounts] = useState(false);
  return (
    <div className="stack">
      <PageHeader
        backTo="/quotations"
        section="Quotation / Proposal"
        title="Bulk Quotation Acceptance"
        description="Upload the ARNs of the quotations your clients accepted; the uploaded list is kept as the acceptance evidence."
        actions={<Link to="/bulk/QUOTATION_CREATE">Bulk quotations</Link>}
      />
      <BulkUploadWizard
        handler="QUOTATION_ACCEPTANCE"
        parameters={{ createAccounts: createAccounts ? 'Y' : 'N' }}
        onCommitted={() => {
          void queryClient.invalidateQueries({ queryKey: ['quotations'] });
          void queryClient.invalidateQueries({ queryKey: ['accounts'] });
        }}
        parameterFields={
          <label className="checkbox">
            <input
              type="checkbox"
              checked={createAccounts}
              onChange={(e) => setCreateAccounts(e.target.checked)}
            />
            Create the accounts at once (confirmed clients only)
          </label>
        }
      />
    </div>
  );
}
