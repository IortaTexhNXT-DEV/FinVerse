import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { csfApi } from '@/api/csf';
import { csfCaseApi } from '@/api/csfCases';
import type { LogCaseBody } from '@/api/csfCases';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { CASE_LOV } from './caseCodes';

const EMPTY: LogCaseBody = {
  channel: '',
  caseType: '',
  nature: '',
  productType: '',
  marketSegment: '',
  fulfilmentUnit: '',
  priority: 'NORMAL',
  subject: '',
  description: '',
  resolveNow: false,
};

/**
 * Log Case (FR-CSF-050, 051, 060): from the Servicing View of a client, or from the Cases list for
 * a caller who is not a client. The fulfilment unit is proposed from the routing table; the case is
 * submitted to the unit or, when the inquiry was answered on the call, resolved and closed.
 */
export function LogCaseDialog({
  companyId,
  client,
  onClose,
}: Readonly<{
  companyId: number;
  client?: { id: number; name: string };
  onClose: () => void;
}>) {
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [body, setBody] = useState<LogCaseBody>({ ...EMPTY, clientId: client?.id });
  const set = (patch: Partial<LogCaseBody>) => setBody((b) => ({ ...b, ...patch }));
  const accounts = useQuery({
    queryKey: ['csf', 'accounts', companyId, client?.id],
    queryFn: () => csfApi.accounts(companyId, client?.id ?? 0),
    enabled: client !== undefined,
  });
  const tats = useQuery({
    queryKey: ['csf-cases', 'tat'],
    queryFn: csfCaseApi.turnaroundTimes,
    staleTime: 5 * 60_000,
  });
  const tat = (tats.data ?? []).find((t) => t.caseType === body.caseType);
  const { caseType, marketSegment, nature } = body;
  useEffect(() => {
    if (caseType === '' && nature === '') {
      return;
    }
    void csfCaseApi
      .propose(companyId, caseType, marketSegment, nature)
      .then((p) => {
        if (p.fulfilmentUnit) {
          setBody((b) => ({ ...b, fulfilmentUnit: p.fulfilmentUnit ?? b.fulfilmentUnit }));
        }
      })
      .catch(() => undefined);
  }, [companyId, caseType, marketSegment, nature]);
  const submit = useMutation({
    mutationFn: (resolveNow: boolean) => csfCaseApi.log(companyId, { ...body, resolveNow }),
    onSuccess: (r) => {
      toast.success(`${r.row.caseNo} logged, due ${r.row.dueDate}`);
      void queryClient.invalidateQueries({ queryKey: ['csf-cases'] });
      onClose();
      void navigate(`/csf/cases/${String(r.row.id)}`);
    },
  });
  const lov = (label: string, type: string, key: keyof LogCaseBody) => (
    <Field label={label} required>
      {(id) => (
        <LovSelect
          id={id}
          type={type}
          value={String(body[key] ?? '')}
          onChange={(value) => set({ [key]: value })}
          required
        />
      )}
    </Field>
  );
  return (
    <Modal
      title={client ? `Log Case - ${client.name}` : 'Log Case'}
      open
      size="lg"
      onClose={onClose}
      helper={tat ? `Turnaround time ${String(tat.tatDays)} working days` : undefined}
      footer={
        <>
          <Button variant="secondary" busy={submit.isPending} onClick={() => submit.mutate(true)}>
            Resolve and Close
          </Button>
          <Button variant="accent" busy={submit.isPending} onClick={() => submit.mutate(false)}>
            Submit to Fulfilment Unit
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={submit.error} title="The case was not logged" />
        {client ? (
          <Field label="Account / Policy">
            {(id) => (
              <select
                id={id}
                className="select"
                value={body.accountId ?? ''}
                onChange={(e) =>
                  set({ accountId: e.target.value ? Number(e.target.value) : undefined })
                }
              >
                <option value="">No account</option>
                {(accounts.data ?? []).map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.arn} {a.policyNumbers[0] ?? ''} {a.productName}
                  </option>
                ))}
              </select>
            )}
          </Field>
        ) : (
          <div className="form-grid">
            <Field label="Caller Name" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  maxLength={200}
                  value={body.callerName ?? ''}
                  onChange={(e) => set({ callerName: e.target.value })}
                />
              )}
            </Field>
            <Field label="Caller Contact" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  placeholder="Mobile or e-mail"
                  maxLength={200}
                  value={body.callerContact ?? ''}
                  onChange={(e) => set({ callerContact: e.target.value })}
                />
              )}
            </Field>
          </div>
        )}
        <div className="form-grid">
          {lov('Channel', CASE_LOV.channel, 'channel')}
          {lov('Case Type', CASE_LOV.type, 'caseType')}
          {lov('Nature of Transaction', CASE_LOV.nature, 'nature')}
          {lov('Product Type', CASE_LOV.product, 'productType')}
          {lov('Market Segment', CASE_LOV.segment, 'marketSegment')}
          {lov('Fulfilment Unit', CASE_LOV.unit, 'fulfilmentUnit')}
          {lov('Priority', CASE_LOV.priority, 'priority')}
        </div>
        <Field label="Subject" required>
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={150}
              value={body.subject}
              onChange={(e) => set({ subject: e.target.value })}
            />
          )}
        </Field>
        <Field label="Description" required>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={4}
              maxLength={4000}
              value={body.description}
              onChange={(e) => set({ description: e.target.value })}
            />
          )}
        </Field>
        <Field label="Resolution" hint="Needed for Resolve and Close">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={4000}
              value={body.resolution ?? ''}
              onChange={(e) => set({ resolution: e.target.value })}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
