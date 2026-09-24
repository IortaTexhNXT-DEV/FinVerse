import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { taxApi } from '@/api/tax';
import type { TaxForm } from '@/api/tax';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { isQuarterly } from './taxDisplay';
import type { PeriodChoice } from './taxPeriods';
import { choiceLabel, periodOf } from './taxPeriods';

interface Props {
  companyId: number;
  forms: TaxForm[];
  choice: PeriodChoice;
  onClose: () => void;
}

/** Prepares a draft return of the selected period for one of the forms of a worksheet. */
export function CreateReturnModal({ companyId, forms, choice, onClose }: Readonly<Props>) {
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const matching = forms.filter((f) => isQuarterly(f) === (choice.granularity === 'QUARTER'));
  const [formCode, setFormCode] = useState(matching[0]?.code ?? '');
  const create = useMutation({
    mutationFn: () => taxApi.createReturn(companyId, formCode, periodOf(choice).from),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['tax-returns'] });
      toast.success(`Return ${r.returnNo} prepared as draft`);
      onClose();
      await navigate('/tax/returns');
    },
  });
  return (
    <Modal
      title={`Create return – ${choiceLabel(choice)}`}
      open
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          disabled={formCode === ''}
          busy={create.isPending}
          onClick={() => create.mutate()}
        >
          Prepare Draft
        </Button>
      }
    >
      <ErrorAlert error={create.error} />
      {matching.length === 0 ? (
        <p className="muted">
          No authorized form of this worksheet is filed per{' '}
          {choice.granularity === 'QUARTER' ? 'quarter' : 'month'}. Change the period type.
        </p>
      ) : (
        <Field label="Form" hint="The figures are frozen when the return is filed.">
          {(id) => (
            <select
              id={id}
              className="select"
              value={formCode}
              onChange={(e) => setFormCode(e.target.value)}
            >
              {matching.map((f) => (
                <option key={f.code} value={f.code}>
                  {f.code} – {f.name}
                </option>
              ))}
            </select>
          )}
        </Field>
      )}
    </Modal>
  );
}
