import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import type { BulkJob } from '@/api/bulk';
import { renewalApi } from '@/api/renewal';
import { Button } from '@/components/ui/Button';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { countOf } from '@/utils/format';
import { UnitSelect } from './ActionDialogs';

/**
 * Declares a committed disposition file complete for an expiry range and unit (BRD 3.004.4): the
 * renewals of the scope missing from the file are tagged "not in the file".
 */
export function CompleteFileForm({ job }: Readonly<{ job: BulkJob }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [unit, setUnit] = useState('');
  const declare = useMutation({
    mutationFn: () =>
      renewalApi.completeFile(job.id, {
        companyId,
        expiryFrom: from,
        expiryTo: to,
        unit: unit.trim(),
      }),
    onSuccess: (r) => toast.success(`${countOf(r.tagged, 'renewal')} tagged not in the file`),
  });
  return (
    <div className="stack">
      <h3>Complete file</h3>
      <p className="muted">
        When the file covers every renewal of a unit for an expiry range, declare it complete: the
        renewals of that scope missing from the file are tagged for follow-up.
      </p>
      <ErrorAlert error={declare.error} />
      <div className="form-grid">
        <Field label="Expiry From" required>
          {(id) => <DateInput id={id} value={from} onChange={(e) => setFrom(e.target.value)} />}
        </Field>
        <Field label="Expiry To" required>
          {(id) => <DateInput id={id} value={to} onChange={(e) => setTo(e.target.value)} />}
        </Field>
        <Field label="Unit" required>
          {(id) => <UnitSelect id={id} value={unit} onChange={(code) => setUnit(code)} />}
        </Field>
      </div>
      <div>
        <Button
          variant="secondary"
          busy={declare.isPending}
          disabled={from === '' || to === '' || unit.trim() === '' || declare.isSuccess}
          onClick={() => declare.mutate()}
        >
          Declare Complete File
        </Button>
      </div>
    </div>
  );
}
