import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { claimsApi } from '@/api/claims';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { LpoDialog } from './LpoDialog';
import { LpoTable } from './LpoTable';
import type { ClaimTabProps } from './ReservesTab';

/** Local purchase orders of a motor claim. */
export function LposTab({ claim, actions, onChange }: Readonly<ClaimTabProps>) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const key = ['claim-docs', claim.id, 'lpos'];
  const lpos = useQuery({ queryKey: key, queryFn: () => claimsApi.lpos(claim.id) });
  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['claim-lpos'] });
    await onChange();
  };
  return (
    <Card
      title="Local purchase orders"
      flush
      actions={
        actions.lpo && (
          <Button
            size="sm"
            variant="accent"
            icon={<Plus size={14} />}
            onClick={() => setOpen(true)}
          >
            Issue LPO
          </Button>
        )
      }
    >
      <ErrorAlert error={lpos.error} />
      {claim.businessLine !== 'MOTOR' && (
        <p className="muted" style={{ margin: 12 }}>
          LPOs are issued for motor garage repairs only.
        </p>
      )}
      <LpoTable lpos={lpos.data ?? []} loading={lpos.isLoading} onChange={refresh} />
      <LpoDialog claim={claim} open={open} onClose={() => setOpen(false)} onSaved={refresh} />
    </Card>
  );
}
