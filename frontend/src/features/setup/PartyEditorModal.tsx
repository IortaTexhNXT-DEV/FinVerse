import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { partiesApi } from '@/api/parties';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { PartyFormFields } from './PartyFormFields';
import { toPartyInput, validateParty } from './partyForm';
import type { PartyForm } from './partyForm';

interface Props {
  initial: PartyForm;
  onClose: () => void;
}

/** Create / edit dialog of a business partner; saving sends it for authorization. */
export function PartyEditorModal({ initial, onClose }: Readonly<Props>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<PartyForm>(initial);
  const [touched, setTouched] = useState(false);
  const save = useMutation({
    mutationFn: (f: PartyForm) =>
      f.id === undefined
        ? partiesApi.create(toPartyInput(f))
        : partiesApi.update(f.id, toPartyInput(f)),
    onSuccess: async (p) => {
      await queryClient.invalidateQueries({ queryKey: ['parties'] });
      toast.success(`Party ${p.code} saved – pending authorization`);
      onClose();
    },
  });
  const errors = touched ? validateParty(form) : {};
  const submit = () => {
    setTouched(true);
    if (Object.keys(validateParty(form)).length === 0) {
      save.mutate(form);
    }
  };

  return (
    <Modal
      title={form.id === undefined ? 'New business partner' : `Edit ${form.code ?? ''}`}
      open
      onClose={onClose}
      footer={
        <Button variant="accent" busy={save.isPending} onClick={submit}>
          Save for authorization
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      <PartyFormFields form={form} errors={errors} onChange={setForm} />
    </Modal>
  );
}
