import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { DEACTIVATION_ENTITY, deactivationApi } from '@/api/pmWorkspace';
import type { Deactivation } from '@/api/pmWorkspace';
import { Attachments } from '@/components/attachments/Attachments';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Combobox } from '@/components/ui/Combobox';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { useToast } from '@/components/ui/toastContext';
import { useDisplayName } from '@/components/ui/useDisplayName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, today } from '@/utils/format';
import { deactivationProblems } from './deactivationForm';
import type { DeactivationForm } from './deactivationForm';

interface DeactivateDialogProps {
  productCode: string;
  packageName: string;
  expiryDate: string | null;
  onClose: () => void;
}

/**
 * Deactivate Package (BDOI FRS FRPM.003.04): the deactivation effective date (today or later), the
 * reason, remarks and the approver; once submitted, the supporting documents are attached to the
 * request and the approver is told.
 */
export function DeactivateDialog({
  productCode,
  packageName,
  expiryDate,
  onClose,
}: Readonly<DeactivateDialogProps>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const name = useDisplayName();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<DeactivationForm>({
    effectiveDate: '',
    reason: '',
    remarks: '',
    approver: '',
  });
  const [tried, setTried] = useState(false);
  const [created, setCreated] = useState<Deactivation | null>(null);
  const settings = useQuery({
    queryKey: ['pm-deactivation', 'settings'],
    queryFn: () => deactivationApi.settings(),
  });
  const problems = tried ? deactivationProblems(form, today()) : {};
  const submit = useMutation({
    mutationFn: () =>
      deactivationApi.submit({
        companyId,
        productCode,
        effectiveDate: form.effectiveDate,
        reason: form.reason,
        remarks: form.remarks || undefined,
        approver: form.approver,
      }),
    onSuccess: async (request) => {
      setCreated(request);
      toast.success(`Deactivation request ${request.requestNo} sent for approval`);
      await queryClient.invalidateQueries({ queryKey: ['pm-matrix'] });
      await queryClient.invalidateQueries({ queryKey: ['pm-deactivations'] });
    },
  });
  const set = (patch: Partial<DeactivationForm>) => setForm((f) => ({ ...f, ...patch }));
  const send = () => {
    setTried(true);
    if (Object.keys(deactivationProblems(form, today())).length === 0) {
      submit.mutate();
    }
  };
  const footer =
    created === null ? (
      <>
        <Button variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button variant="danger" busy={submit.isPending} onClick={send}>
          Submit for Approval
        </Button>
      </>
    ) : (
      <Button onClick={onClose}>Done</Button>
    );
  return (
    <Modal
      title="Deactivate Package"
      open
      onClose={onClose}
      footer={footer}
      size="md"
      facts={[
        { label: 'Package', value: `${packageName} (${productCode})` },
        { label: 'Expiry Date', value: formatDate(expiryDate) || 'None' },
      ]}
    >
      {created === null ? (
        <div className="form-grid">
          <Field label="Deactivation Effective Date" required error={problems.effectiveDate}>
            {(id) => (
              <DateInput
                id={id}
                min={today()}
                value={form.effectiveDate}
                onChange={(e) => set({ effectiveDate: e.target.value })}
              />
            )}
          </Field>
          <Field label="Reason for Deactivation" required error={problems.reason}>
            {(id) => (
              <LovSelect
                id={id}
                type="PKG_DEACTIVATION_REASON"
                value={form.reason}
                onChange={(reason) => set({ reason })}
                required
              />
            )}
          </Field>
          <Field label="Approver" required error={problems.approver}>
            {(id) => (
              <Combobox
                id={id}
                placeholder="Select the approver"
                value={form.approver}
                loading={settings.isLoading}
                options={(settings.data?.approvers ?? []).map((u) => ({
                  value: u,
                  label: name(u),
                }))}
                onChange={(approver) => set({ approver })}
              />
            )}
          </Field>
          <Field label="Remarks">
            {(id) => (
              <textarea
                id={id}
                className="input"
                rows={3}
                maxLength={1000}
                value={form.remarks}
                onChange={(e) => set({ remarks: e.target.value })}
              />
            )}
          </Field>
          <ErrorAlert error={submit.error ?? settings.error} />
        </div>
      ) : (
        <div className="stack">
          <Notice tone="success">
            Deactivation request {created.requestNo} was sent to {name(created.approver)} for
            approval. Attach the supporting documents below if any.
          </Notice>
          <Attachments
            entityType={DEACTIVATION_ENTITY}
            entityId={created.id}
            title="Supporting Documents"
            reference={created.requestNo}
          />
        </div>
      )}
    </Modal>
  );
}
