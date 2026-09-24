import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import type { Insurer, InsurerInput, PlacementChannel } from '@/api/catalog';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';
import { splitEmails } from './insurerForm';

const CHANNELS = [
  { value: 'EMAIL', label: 'E-mail' },
  { value: 'SFTP', label: 'SFTP (not yet available)' },
  { value: 'API', label: 'API (not yet available)' },
];

type InsurerForm = Omit<InsurerInput, 'defaultCreditDays' | 'placementEmails'> & {
  defaultCreditDays?: number;
  emails: string;
};

function formOf(companyId: number, insurer?: Insurer): InsurerForm {
  if (insurer === undefined) {
    return {
      companyId,
      partyCode: 'INS-',
      name: '',
      placementChannel: 'EMAIL',
      defaultCreditDays: 30,
      emails: '',
    };
  }
  return {
    companyId,
    partyCode: insurer.partyCode,
    name: insurer.name,
    shortName: insurer.shortName ?? undefined,
    accreditationNo: insurer.accreditationNo ?? undefined,
    accreditedUntil: insurer.accreditedUntil ?? undefined,
    placementChannel: insurer.placementChannel,
    defaultCreditDays: insurer.defaultCreditDays,
    emails: insurer.placementEmails.join(', '),
  };
}

interface Props {
  companyId: number;
  insurer?: Insurer;
  onClose: () => void;
}

/**
 * Accredits an insurer on the panel or changes its profile. A new insurer also creates its
 * business partner (party type Insurer) for the ledger; both are authorized together.
 */
export function InsurerEditorModal({ companyId, insurer, onClose }: Readonly<Props>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState(() => formOf(companyId, insurer));
  const set = (patch: Partial<InsurerForm>) => setForm((f) => ({ ...f, ...patch }));
  const save = useMutation({
    mutationFn: () => {
      const { emails, ...rest } = form;
      const body: InsurerInput = {
        ...rest,
        defaultCreditDays: rest.defaultCreditDays ?? 0,
        placementEmails: splitEmails(emails),
      };
      return insurer === undefined
        ? catalogApi.createInsurer(body)
        : catalogApi.updateInsurer(insurer.id, body);
    },
    onSuccess: async (i) => {
      await queryClient.invalidateQueries({ queryKey: ['catalog'] });
      toast.success(`${i.name} saved – pending authorization`);
      onClose();
    },
  });
  return (
    <Modal
      title={insurer === undefined ? 'New insurer' : `Edit ${insurer.name}`}
      open
      onClose={onClose}
      footer={
        <Button variant="accent" busy={save.isPending} onClick={() => save.mutate()}>
          Save for Authorization
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <TextInput
          label="Partner code"
          required
          upper
          disabled={insurer !== undefined}
          value={form.partyCode}
          onChange={(partyCode) => set({ partyCode })}
        />
        <TextInput label="Name" required value={form.name} onChange={(name) => set({ name })} />
        <TextInput
          label="Short name"
          value={form.shortName}
          onChange={(shortName) => set({ shortName })}
        />
        <TextInput
          label="IC accreditation no."
          value={form.accreditationNo}
          onChange={(accreditationNo) => set({ accreditationNo })}
        />
        <TextInput
          label="Accredited until"
          type="date"
          value={form.accreditedUntil}
          onChange={(accreditedUntil) => set({ accreditedUntil: accreditedUntil || undefined })}
        />
        <SelectInput
          label="Placement channel"
          required
          value={form.placementChannel}
          options={CHANNELS}
          onChange={(v) => set({ placementChannel: v as PlacementChannel })}
        />
        <TextInput
          label="Placement e-mails"
          hint="Separate addresses with commas"
          value={form.emails}
          onChange={(emails) => set({ emails })}
        />
        <NumberInput
          label="Credit days"
          step="1"
          value={form.defaultCreditDays}
          onChange={(defaultCreditDays) => set({ defaultCreditDays })}
        />
        {insurer === undefined && (
          <>
            <TextInput label="TIN" value={form.taxId} onChange={(taxId) => set({ taxId })} />
            <TextInput
              label="Address"
              value={form.address}
              onChange={(address) => set({ address })}
            />
            <TextInput label="E-mail" value={form.email} onChange={(email) => set({ email })} />
            <TextInput label="Phone" value={form.phone} onChange={(phone) => set({ phone })} />
          </>
        )}
      </div>
    </Modal>
  );
}
