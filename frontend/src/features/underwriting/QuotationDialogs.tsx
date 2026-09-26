import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { underwritingApi } from '@/api/underwriting';
import type { ConvertInput, IterationInput, Quotation } from '@/api/underwriting';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { today } from '@/utils/format';
import { CheckboxField, DateField, SelectField, TextField } from './FormFields';
import { IterationFields } from './IterationFields';
import { iterationDefaults } from './policyForm';
import { useUwLookups } from './useUwLookups';

interface DialogProps {
  quotation: Quotation;
  open: boolean;
  onClose: () => void;
}

/** New negotiation iteration, prefilled with the current figures. */
export function IterateDialog({
  quotation: q,
  open,
  onClose,
  busy,
  error,
  onSave,
}: Readonly<
  DialogProps & { busy: boolean; error: unknown; onSave: (value: IterationInput) => void }
>) {
  const [iteration, setIteration] = useState<IterationInput>(() => iterationDefaults(q));
  return (
    <Modal
      title="New Iteration"
      open={open}
      onClose={onClose}
      footer={
        <Button variant="accent" busy={busy} onClick={() => onSave(iteration)}>
          Save Iteration
        </Button>
      }
    >
      <ErrorAlert error={error} />
      <IterationFields value={iteration} sharePct={q.sharePct} onChange={setIteration} />
    </Modal>
  );
}

/** Conversion of an approved quotation into a draft policy. */
export function ConvertDialog({ quotation: q, open, onClose }: Readonly<DialogProps>) {
  const lookups = useUwLookups();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [convert, setConvert] = useState<ConvertInput>({
    issueDate: today(),
    coinsuranceLeader: false,
  });
  const coinsured = q.sharePct < 100;
  const converter = useMutation({
    mutationFn: (body: ConvertInput) => underwritingApi.convertQuotation(q.id, body),
    onSuccess: async (policy) => {
      toast.success(`Converted into draft policy ${policy.policyNo}`);
      await queryClient.invalidateQueries({ queryKey: ['quotations'] });
      await navigate(`/underwriting/policies/${String(policy.id)}`);
    },
  });
  return (
    <Modal
      title={`Convert ${q.quotationNo}`}
      open={open}
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          busy={converter.isPending}
          onClick={() => converter.mutate(convert)}
        >
          Create Draft Policy
        </Button>
      }
    >
      <div className="stack">
        <ErrorAlert error={converter.error} />
        <div className="form-grid">
          <DateField
            label="Policy issue date"
            required
            value={convert.issueDate}
            onChange={(v) => setConvert({ ...convert, issueDate: v })}
          />
          <TextField
            label="Risk description"
            value={convert.riskDescription}
            onChange={(v) => setConvert({ ...convert, riskDescription: v })}
          />
          {coinsured && (
            <SelectField
              label="Coinsurer"
              required
              value={convert.coinsurerCode}
              emptyLabel="Select coinsurer"
              options={lookups.coinsurers.map((c) => ({ value: c.code, label: c.name }))}
              onChange={(v) => setConvert({ ...convert, coinsurerCode: v })}
            />
          )}
          {coinsured && (
            <CheckboxField
              label="We lead (bill 100%)"
              checked={convert.coinsuranceLeader}
              onChange={(v) => setConvert({ ...convert, coinsuranceLeader: v })}
            />
          )}
        </div>
      </div>
    </Modal>
  );
}
