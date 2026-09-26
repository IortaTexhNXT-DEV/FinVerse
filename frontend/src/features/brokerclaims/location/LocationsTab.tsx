import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { MapPinPlus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { coverApi } from '../cover/api';
import type { Claim } from '../record/api';
import { DialogFooter } from '../record/FormParts';
import type { ClaimLocation, LocationPick } from './api';
import { locationApi } from './api';
import { LocationPicker } from './LocationPicker';

function AddLocationsDialog({
  claim,
  companyId,
  linked,
  onClose,
  onDone,
}: Readonly<{
  claim: Claim;
  companyId: number;
  linked: number[];
  onClose: () => void;
  onDone: () => void;
}>) {
  const [picks, setPicks] = useState<LocationPick[]>([]);
  const draft = useQuery({
    queryKey: ['broker-claims', 'draft', companyId, claim.cover.arn, claim.cover.policyYear],
    queryFn: () => coverApi.draft(companyId, claim.cover.arn, claim.cover.policyYear),
  });
  const link = useMutation({
    mutationFn: () => locationApi.link(companyId, claim.id, picks),
    onSuccess: onDone,
  });
  return (
    <Modal
      title="Add Locations"
      open
      onClose={onClose}
      footer={
        <DialogFooter
          label="Add Locations"
          busy={link.isPending}
          disabled={picks.length === 0}
          onClose={onClose}
          onConfirm={() => link.mutate()}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={draft.error ?? link.error} />
        {draft.data && (
          <LocationPicker
            locations={draft.data.locations}
            picks={picks}
            onChange={setPicks}
            exclude={linked}
          />
        )}
      </div>
    </Modal>
  );
}

/**
 * Locations tab of a claim (BRCLM.037/042; FR-CL-020/023): the insured locations of the cover
 * linked to the claim with their address and the reference of each insurer valid today; links and
 * removals while the claim is open (BCL_RECORD), each recorded in the claim history.
 */
export function LocationsTab({ claim, companyId }: Readonly<{ claim: Claim; companyId: number }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [adding, setAdding] = useState(false);
  const [removing, setRemoving] = useState<ClaimLocation>();
  const key = ['broker-claims', 'locations', claim.id];
  const locations = useQuery({
    queryKey: key,
    queryFn: () => locationApi.ofClaim(companyId, claim.id),
  });
  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: key });
    await queryClient.invalidateQueries({ queryKey: ['broker-claims', 'claim', claim.id] });
  };
  const remove = useMutation({
    mutationFn: (itemNo: number) => locationApi.remove(companyId, claim.id, itemNo),
    onSuccess: async () => {
      toast.success('Location removed');
      setRemoving(undefined);
      await refresh();
    },
  });
  const editable = claim.progress.phase !== 'CLOSED' && can('BCL_RECORD');
  const rows = locations.data ?? [];
  return (
    <div className="stack">
      {editable && (
        <div className="row">
          <Button
            variant="secondary"
            icon={<MapPinPlus size={16} />}
            onClick={() => setAdding(true)}
          >
            Add Locations
          </Button>
        </div>
      )}
      <ErrorAlert error={locations.error} />
      <DataTable<ClaimLocation>
        caption="Locations"
        loading={locations.isLoading}
        rows={rows}
        rowKey={(l) => l.itemNo}
        emptyMessage={`No location linked. Place of loss: ${claim.loss.lossPlace ?? 'not entered'}.`}
        columns={[
          { key: 'i', header: 'Item', render: (l) => l.itemNo },
          { key: 'a', header: 'Address', render: (l) => l.address ?? '' },
          {
            key: 'c',
            header: 'City / Province',
            render: (l) => [l.city, l.province].filter(Boolean).join(', '),
          },
          { key: 'd', header: 'Damage', render: (l) => l.description ?? '' },
          {
            key: 'r',
            header: 'Insurer References',
            render: (l) =>
              l.references.length === 0
                ? '—'
                : l.references.map((r) => `${r.insurerCode}: ${r.reference}`).join('; '),
          },
          {
            key: 'x',
            header: '',
            render: (l) =>
              editable ? (
                <Button
                  variant="ghost"
                  size="sm"
                  aria-label={`Remove location ${l.itemNo}`}
                  icon={<Trash2 size={14} />}
                  onClick={() => setRemoving(l)}
                />
              ) : null,
          },
        ]}
      />
      {adding && (
        <AddLocationsDialog
          claim={claim}
          companyId={companyId}
          linked={rows.map((l) => l.itemNo)}
          onClose={() => setAdding(false)}
          onDone={() => {
            toast.success('Locations added');
            setAdding(false);
            void refresh();
          }}
        />
      )}
      {removing !== undefined && (
        <Modal
          title="Remove Location"
          open
          onClose={() => setRemoving(undefined)}
          footer={
            <DialogFooter
              label="Remove Location"
              busy={remove.isPending}
              onClose={() => setRemoving(undefined)}
              onConfirm={() => remove.mutate(removing.itemNo)}
            />
          }
        >
          <div className="stack">
            <ErrorAlert error={remove.error} />
            <p>
              Remove item {removing.itemNo} ({removing.address}) from claim {claim.claimNo}? The
              removal is kept in the claim history.
            </p>
          </div>
        </Modal>
      )}
    </div>
  );
}
