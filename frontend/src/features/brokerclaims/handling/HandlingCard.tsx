import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Car, Undo2 } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { DialogFooter, InputField, TextAreaField } from '../record/FormParts';
import type { Claim, ClaimExtras } from '../record/api';
import { handlingApi } from './api';

function MotorDialog({
  claim,
  companyId,
  onClose,
}: Readonly<{ claim: Claim; companyId: number; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [plate, setPlate] = useState(claim.extras?.plateNo ?? '');
  const [shop, setShop] = useState(claim.extras?.preferredShopCode ?? '');
  const shops = useQuery({
    queryKey: ['broker-claims', 'repair-shops', companyId],
    queryFn: () => handlingApi.repairShops(companyId),
  });
  const save = useMutation({
    mutationFn: () => handlingApi.motor(companyId, claim.id, plate, shop),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['broker-claims', 'claim', claim.id] });
      toast.success('Motor details saved');
      onClose();
    },
  });
  return (
    <Modal
      title="Motor Details"
      open
      onClose={onClose}
      footer={
        <DialogFooter
          label="Save"
          busy={save.isPending}
          onClose={onClose}
          onConfirm={() => save.mutate()}
        />
      }
    >
      <ErrorAlert error={save.error ?? shops.error} />
      <div className="form-grid">
        <InputField label="Plate number" maxLength={20} value={plate} onChange={setPlate} />
        <Field label="Preferred repair shop">
          {(id) => (
            <select
              id={id}
              className="select"
              value={shop}
              onChange={(e) => setShop(e.target.value)}
            >
              <option value="">None</option>
              {(shops.data ?? []).map((s) => (
                <option key={s.code} value={s.code}>
                  {s.name} ({s.code})
                </option>
              ))}
            </select>
          )}
        </Field>
      </div>
    </Modal>
  );
}

function ReturnDialog({
  claim,
  companyId,
  onClose,
}: Readonly<{ claim: Claim; companyId: number; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [note, setNote] = useState('');
  const save = useMutation({
    mutationFn: () => handlingApi.returnToAo(companyId, claim.id, note),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['broker-claims'] });
      toast.success('Claim returned to the Account Officer');
      onClose();
    },
  });
  return (
    <Modal
      title="Return to Account Officer"
      open
      onClose={onClose}
      footer={
        <DialogFooter
          label="Return for Premium Collection"
          busy={save.isPending}
          onClose={onClose}
          onConfirm={() => save.mutate()}
        />
      }
    >
      <ErrorAlert error={save.error} />
      <p>
        The premium of the policy is not fully paid. The Account Officer{' '}
        <UserName login={claim.cover.accountOfficer} /> is notified to collect it; the authorization
        code stays disabled until it is paid.
      </p>
      <TextAreaField label="Note" maxLength={500} value={note} onChange={setNote} />
    </Modal>
  );
}

/**
 * Plate number and preferred repair shop of the claim (FRS BRD-07 FRCLM.008.02, 007.02) and the
 * return of the claim of a partly paid or unpaid policy to the Account Officer (FRCLM.008.04).
 */
function HandlingActions({
  claim,
  onOpen,
}: Readonly<{ claim: Claim; onOpen: (dialog: 'motor' | 'return') => void }>) {
  const { can } = useAuth();
  if (claim.progress.phase === 'CLOSED') {
    return null;
  }
  const canReturn = claim.flags.unpaidPremium && (can('BCL_RECORD') || can('BCL_STATUS_UPDATE'));
  return (
    <>
      {can('BCL_RECORD') && (
        <Button
          size="sm"
          variant="secondary"
          icon={<Car size={14} />}
          onClick={() => onOpen('motor')}
        >
          Motor Details
        </Button>
      )}
      {canReturn && (
        <Button
          size="sm"
          variant="secondary"
          icon={<Undo2 size={14} />}
          onClick={() => onOpen('return')}
        >
          Return to Account Officer
        </Button>
      )}
    </>
  );
}

function ReturnedText({ extras }: Readonly<{ extras?: ClaimExtras }>) {
  if (!extras?.returnedToAoAt) {
    return <>—</>;
  }
  return (
    <>
      {formatDateTime(extras.returnedToAoAt)} by <UserName login={extras.returnedToAoBy} />
      {extras.returnedToAoNote ? ` – ${extras.returnedToAoNote}` : ''}
    </>
  );
}

/**
 * Plate number and preferred repair shop of the claim (FRS BRD-07 FRCLM.008.02, 007.02) and the
 * return of the claim of a partly paid or unpaid policy to the Account Officer (FRCLM.008.04).
 */
export function HandlingCard({ claim, companyId }: Readonly<{ claim: Claim; companyId: number }>) {
  const [dialog, setDialog] = useState<'motor' | 'return'>();
  const e = claim.extras;
  return (
    <Card title="Claim Handling" actions={<HandlingActions claim={claim} onOpen={setDialog} />}>
      <dl className="detail-list">
        <dt>Plate number</dt>
        <dd>{e?.plateNo ?? '—'}</dd>
        <dt>Preferred repair shop</dt>
        <dd>{e?.preferredShopCode ?? '—'}</dd>
        <dt>Returned to the Account Officer</dt>
        <dd>
          <ReturnedText extras={e} />
        </dd>
      </dl>
      {dialog === 'motor' && (
        <MotorDialog claim={claim} companyId={companyId} onClose={() => setDialog(undefined)} />
      )}
      {dialog === 'return' && (
        <ReturnDialog claim={claim} companyId={companyId} onClose={() => setDialog(undefined)} />
      )}
    </Card>
  );
}
