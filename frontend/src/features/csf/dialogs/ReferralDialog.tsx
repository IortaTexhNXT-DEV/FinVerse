import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { csfApi } from '@/api/csf';
import { lovApi } from '@/api/lov';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { CSF_LOV } from '../csfCodes';
import '../csf.css';

/**
 * Refer to Fulfilment Unit (FRS FR-CSF-021; e-mail topic 3): a change the contact centre cannot
 * make - name, civil status, birth date, ID, TIN - is recorded with the value the client asked
 * for and handed to the fulfilment unit, which is notified.
 */
export function ReferralDialog({
  companyId,
  clientId,
  clientName,
  onClose,
}: Readonly<{ companyId: number; clientId: number; clientName: string; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [channel, setChannel] = useState('');
  const [asked, setAsked] = useState<Record<string, string>>({});
  const [remarks, setRemarks] = useState('');
  const fields = useQuery({
    queryKey: ['lov', CSF_LOV.referral],
    queryFn: () => lovApi.options(CSF_LOV.referral),
    staleTime: 5 * 60_000,
  });
  const chosen = Object.keys(asked);
  const refer = useMutation({
    mutationFn: () =>
      csfApi.refer(companyId, clientId, {
        channel,
        fields: asked,
        remarks: remarks.trim() || undefined,
      }),
    onSuccess: (c) => {
      toast.success(`${c.changeNo} referred to the fulfilment unit`);
      void queryClient.invalidateQueries({ queryKey: ['csf'] });
      onClose();
    },
  });
  const toggle = (code: string, on: boolean) =>
    setAsked(
      on
        ? { ...asked, [code]: '' }
        : Object.fromEntries(Object.entries(asked).filter(([k]) => k !== code)),
    );
  return (
    <Modal
      title={`Refer to Fulfilment Unit - ${clientName}`}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            disabled={channel === '' || chosen.length === 0}
            busy={refer.isPending}
            onClick={() => refer.mutate()}
          >
            Refer
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={refer.error ?? fields.error} title="Cannot refer the change" />
        <Field label="Channel" required>
          {(id) => (
            <LovSelect
              id={id}
              type={CSF_LOV.channel}
              value={channel}
              onChange={setChannel}
              required
            />
          )}
        </Field>
        <fieldset className="csf-checklist" aria-label="Information to change">
          {(fields.data ?? []).map((f) => (
            <div key={f.code} className="csf-check-row">
              <label className="checkbox">
                <input
                  type="checkbox"
                  checked={f.code in asked}
                  onChange={(e) => toggle(f.code, e.target.checked)}
                />
                {f.label}
              </label>
              {f.code in asked && (
                <input
                  className="input"
                  aria-label={`${f.label} asked for`}
                  placeholder="Value asked for"
                  maxLength={300}
                  value={asked[f.code]}
                  onChange={(e) => setAsked({ ...asked, [f.code]: e.target.value })}
                />
              )}
            </div>
          ))}
        </fieldset>
        <Field label="Remarks">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={500}
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
