import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { productMaintApi } from '@/api/productmaint';
import type { CoverageTerm, InsurerResponse, ResponseInput } from '@/api/productmaint';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { NumberInput, TextInput } from '@/features/assets/FormControls';
import { offered } from './packageRequest';

interface ResponseDialogProps {
  requestId: number;
  response: InsurerResponse;
  /** Requested coverages, the starting point of a first response. */
  requested: CoverageTerm[];
  onClose: () => void;
  onSaved: () => void;
}

/**
 * Keys in one insurer's outcome and terms (BRPM.013, PMADD04): the outcome with its exception
 * states, rate, minimum premium, deductibles per coverage, conditions, validity and remarks; the
 * response document is attached separately.
 */
export function ResponseDialog({
  requestId,
  response,
  requested,
  onClose,
  onSaved,
}: Readonly<ResponseDialogProps>) {
  const [form, setForm] = useState<ResponseInput>({
    outcome: response.outcome === 'PENDING' ? '' : response.outcome,
    rate: response.rate,
    minimumPremium: response.minimumPremium,
    coverages: response.coverages.length > 0 ? response.coverages : requested,
    conditions: response.conditions,
    validUntil: response.validUntil,
    remarks: response.remarks,
  });
  const [file, setFile] = useState<File | undefined>();
  const set = (patch: Partial<ResponseInput>) => setForm({ ...form, ...patch });
  const rateMissing = form.outcome !== '' && offered(form.outcome) && form.rate === undefined;
  const save = useMutation({
    mutationFn: async () => {
      await productMaintApi.recordResponse(requestId, response.id, form);
      if (file !== undefined) {
        await productMaintApi.attachResponse(requestId, response.id, file);
      }
    },
    onSuccess: onSaved,
  });
  return (
    <Modal
      open
      title={`Terms of ${response.insurerName}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={save.isPending}
            disabled={form.outcome === '' || rateMissing}
            onClick={() => save.mutate()}
          >
            Save Terms
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <div className="form-grid">
          <Field label="Outcome" required>
            {(id) => (
              <LovSelect
                id={id}
                type="PKG_RESPONSE_OUTCOME"
                value={form.outcome}
                onChange={(outcome) => set({ outcome })}
              />
            )}
          </Field>
          <NumberInput
            label="Rate %"
            required={form.outcome !== '' && offered(form.outcome)}
            step="0.0001"
            error={rateMissing ? 'Enter the rate offered' : undefined}
            value={form.rate}
            onChange={(rate) => set({ rate })}
          />
          <NumberInput
            label="Minimum premium"
            value={form.minimumPremium}
            onChange={(minimumPremium) => set({ minimumPremium })}
          />
          <TextInput
            label="Valid until"
            type="date"
            value={form.validUntil}
            onChange={(v) => set({ validUntil: v === '' ? undefined : v })}
          />
        </div>
        {form.coverages.length > 0 && (
          <div className="form-grid">
            {form.coverages.map((c, index) => (
              <TextInput
                key={c.coverageCode}
                label={`${c.coverageCode} deductible`}
                value={c.deductibleText}
                onChange={(deductibleText) =>
                  set({
                    coverages: form.coverages.map((x, i) =>
                      i === index ? { ...x, deductibleText: deductibleText || undefined } : x,
                    ),
                  })
                }
              />
            ))}
          </div>
        )}
        <TextInput
          label="Conditions and warranties"
          value={form.conditions}
          onChange={(conditions) => set({ conditions })}
        />
        <TextInput label="Remarks" value={form.remarks} onChange={(remarks) => set({ remarks })} />
        <Field label="Response document" hint="The insurer's reply (PDF, e-mail or image).">
          {(id) => <input id={id} type="file" onChange={(e) => setFile(e.target.files?.[0])} />}
        </Field>
      </div>
    </Modal>
  );
}
