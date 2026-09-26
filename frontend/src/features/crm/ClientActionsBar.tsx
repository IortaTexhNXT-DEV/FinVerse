import { useMutation, useQueryClient } from '@tanstack/react-query';
import { BadgeCheck, Ban, Pencil, Send, ShieldCheck } from 'lucide-react';
import type { ReactNode } from 'react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { clientsApi } from '@/api/clients';
import type { ClientAction, ClientDetail } from '@/api/clients';
import { useAuth } from '@/auth/authContext';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { workflowKey } from '@/components/broking/workflowKey';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/toastContext';
import { clientActions } from './clientLabels';

type ActionId = 'submitKyc' | 'verifyKyc' | 'confirm' | 'deactivate';

interface ActionDef {
  title: string;
  confirmLabel: string;
  reasonLov?: string;
  icon: ReactNode;
  run: (id: number, note: ClientAction) => Promise<ClientDetail>;
  done: (c: ClientDetail) => string;
}

const ACTIONS: Record<ActionId, ActionDef> = {
  submitKyc: {
    title: 'Submit the KYC for verification',
    confirmLabel: 'Submit KYC',
    icon: <Send size={16} />,
    run: clientsApi.submitKyc,
    done: (c) => `${c.code}: KYC submitted for verification`,
  },
  verifyKyc: {
    title: 'Verify the KYC',
    confirmLabel: 'Verify KYC',
    icon: <ShieldCheck size={16} />,
    run: clientsApi.verifyKyc,
    done: (c) => `${c.code}: KYC verified, next review ${c.kyc.reviewDue ?? ''}`,
  },
  confirm: {
    title: 'Confirm the client',
    confirmLabel: 'Confirm client',
    icon: <BadgeCheck size={16} />,
    run: clientsApi.confirm,
    done: (c) => `Client confirmed as ${c.clientCode ?? c.code}`,
  },
  deactivate: {
    title: 'Deactivate the client',
    confirmLabel: 'Deactivate',
    reasonLov: 'CLIENT_DEACTIVATION_REASON',
    icon: <Ban size={16} />,
    run: clientsApi.deactivate,
    done: (c) => `${c.code} deactivated`,
  },
};

const ORDER: ActionId[] = ['submitKyc', 'verifyKyc', 'confirm', 'deactivate'];

/**
 * Onboarding actions of the client page (BRNB.090/101), shown by permission and stage: submit the
 * KYC, verify it (checker, not the maker), confirm the client, deactivate with a reason.
 */
export function ClientActionsBar({ client }: Readonly<{ client: ClientDetail }>) {
  const { can, user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<ActionId | null>(null);
  const allowed = clientActions(client, can, user?.username);
  const act = useMutation({
    mutationFn: ({ id, note }: { id: ActionId; note: ClientAction }) =>
      ACTIONS[id].run(client.id, note),
    onSuccess: async (updated, { id }) => {
      setPending(null);
      toast.success(ACTIONS[id].done(updated));
      await queryClient.invalidateQueries({ queryKey: ['crm'] });
      await queryClient.invalidateQueries({ queryKey: workflowKey('Client', client.id) });
    },
  });
  const def = pending === null ? undefined : ACTIONS[pending];
  return (
    <>
      {allowed.edit && (
        <Button
          variant="secondary"
          icon={<Pencil size={16} />}
          onClick={() => void navigate(`/crm/clients/${String(client.id)}/edit`)}
        >
          Edit
        </Button>
      )}
      {ORDER.filter((id) => allowed[id]).map((id) => (
        <Button
          key={id}
          variant={id === 'deactivate' ? 'danger' : 'primary'}
          icon={ACTIONS[id].icon}
          onClick={() => {
            act.reset();
            setPending(id);
          }}
        >
          {ACTIONS[id].confirmLabel}
        </Button>
      ))}
      {pending !== null && def !== undefined && (
        <ActionDialog
          title={def.title}
          reasonLov={def.reasonLov}
          confirmLabel={def.confirmLabel}
          busy={act.isPending}
          error={act.error}
          onClose={() => setPending(null)}
          onConfirm={(note) => act.mutate({ id: pending, note })}
        />
      )}
    </>
  );
}
