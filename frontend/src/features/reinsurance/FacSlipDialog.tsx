import { useMutation } from '@tanstack/react-query';
import { Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { reinsuranceApi } from '@/api/reinsurance';
import type { FacParticipantLine, FacPlacement } from '@/api/reinsurance';
import type { Party } from '@/api/parties';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { NumberField, SelectField, TextField } from '@/features/underwriting/FormFields';
import { formatAmount } from '@/utils/format';
import { facActions, placedShare } from './fac';

interface Props {
  placement: FacPlacement;
  reinsurers: Party[];
  onClose: () => void;
  onChanged: (p: FacPlacement, message: string) => void;
}

type Line = FacParticipantLine & { key: number };

/** Facultative slip: record reinsurers, submit, approve (place), return or close. */
export function FacSlipDialog({ placement, reinsurers, onClose, onChanged }: Readonly<Props>) {
  const { can, user } = useAuth();
  const [lines, setLines] = useState<Line[]>(
    placement.participants.map((p, i) => ({ ...p, key: i })),
  );
  const [remarks, setRemarks] = useState(placement.remarks ?? '');
  const act = useMutation({
    mutationFn: async (action: 'save' | 'submit' | 'approve' | 'reject' | 'close') => {
      const id = placement.id;
      const body = lines.map(({ reinsurerCode, sharePct, commissionPct }) => ({
        reinsurerCode,
        sharePct,
        commissionPct,
      }));
      const calls = {
        save: () => reinsuranceApi.assignPlacement(id, body, remarks),
        submit: async () => {
          await reinsuranceApi.assignPlacement(id, body, remarks);
          return reinsuranceApi.submitPlacement(id);
        },
        approve: () => reinsuranceApi.approvePlacement(id),
        reject: () => reinsuranceApi.rejectPlacement(id),
        close: () => reinsuranceApi.closePlacement(id),
      };
      return { result: await calls[action](), action };
    },
    onSuccess: ({ result, action }) =>
      onChanged(result, `Slip ${result.placementNo}: ${action} done`),
  });
  const allowed = facActions(placement, user?.username, can);
  const editable = allowed.edit;
  const update = (key: number, patch: Partial<Line>) =>
    setLines(lines.map((l) => (l.key === key ? { ...l, ...patch } : l)));

  return (
    <Modal
      title={`Facultative slip ${placement.placementNo}`}
      open
      onClose={onClose}
      footer={
        <div className="row">
          {editable && (
            <>
              <Button variant="secondary" busy={act.isPending} onClick={() => act.mutate('save')}>
                Save
              </Button>
              <Button
                variant="accent"
                busy={act.isPending}
                disabled={lines.length === 0}
                onClick={() => act.mutate('submit')}
              >
                Submit for approval
              </Button>
            </>
          )}
          {allowed.approve && (
            <>
              <Button variant="secondary" busy={act.isPending} onClick={() => act.mutate('reject')}>
                Return to maker
              </Button>
              <Button variant="accent" busy={act.isPending} onClick={() => act.mutate('approve')}>
                Approve placement
              </Button>
            </>
          )}
          {allowed.close && (
            <Button variant="secondary" busy={act.isPending} onClick={() => act.mutate('close')}>
              Close slip
            </Button>
          )}
        </div>
      }
    >
      <ErrorAlert error={act.error} />
      <p>
        Policy <strong>{placement.policyNo}</strong>, risk {placement.riskLineNo}{' '}
        {placement.riskDescription} <StatusBadge status={placement.status} />
      </p>
      <p className="muted">
        Facultative requirement {formatAmount(placement.facSi)} SI ({placement.facPct}% of our{' '}
        {formatAmount(placement.riskSi)}), premium {formatAmount(placement.facPremium)}{' '}
        {placement.currency}. Placed {placedShare(lines)}% of the requirement; the rest stays with
        the company.
      </p>
      {lines.map((l) => (
        <div key={l.key} className="form-grid">
          <SelectField
            label="Reinsurer"
            required
            disabled={!editable}
            value={l.reinsurerCode}
            emptyLabel="Select reinsurer"
            options={reinsurers.map((p) => ({ value: p.code, label: `${p.code} ${p.name}` }))}
            onChange={(v) => update(l.key, { reinsurerCode: v })}
          />
          <NumberField
            label="Share of FAC %"
            required
            disabled={!editable}
            value={l.sharePct}
            onChange={(v) => update(l.key, { sharePct: v ?? 0 })}
          />
          <NumberField
            label="Commission %"
            disabled={!editable}
            value={l.commissionPct}
            onChange={(v) => update(l.key, { commissionPct: v })}
          />
          {editable && (
            <Button
              size="sm"
              variant="ghost"
              aria-label="Remove reinsurer"
              icon={<Trash2 size={14} />}
              onClick={() => setLines(lines.filter((x) => x.key !== l.key))}
            />
          )}
        </div>
      ))}
      {editable && (
        <div className="row">
          <Button
            size="sm"
            variant="secondary"
            icon={<Plus size={14} />}
            onClick={() =>
              setLines([
                ...lines,
                { key: Date.now(), reinsurerCode: '', sharePct: 0, commissionPct: 0 },
              ])
            }
          >
            Add reinsurer
          </Button>
          <TextField label="Remarks" disabled={!editable} value={remarks} onChange={setRemarks} />
        </div>
      )}
      {placement.participants.some((p) => p.premium !== 0) && (
        <p>
          Placed premium <Amount value={placement.placedPremium} />, commission{' '}
          <Amount value={placement.commission} />
        </p>
      )}
    </Modal>
  );
}
