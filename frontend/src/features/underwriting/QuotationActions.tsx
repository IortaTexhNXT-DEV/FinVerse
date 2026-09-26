import { useMutation, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, FileOutput, Layers, Send, XCircle } from 'lucide-react';
import { useState } from 'react';
import { underwritingApi } from '@/api/underwriting';
import type { Quotation } from '@/api/underwriting';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { DecisionDialog } from './DecisionDialog';
import { ConvertDialog, IterateDialog } from './QuotationDialogs';
import { allowedActions, canIterate } from './workflow';

type Dialog = 'iterate' | 'reject' | 'convert' | null;

/** Quotation actions: iterate, submit, approve / reject (checker) and convert to a policy. */
export function QuotationActions({ quotation: q }: Readonly<{ quotation: Quotation }>) {
  const { user, can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [dialog, setDialog] = useState<Dialog>(null);
  const allowed = allowedActions(q, user?.username, can);
  const maintain = can('POLICY_MAINTAIN');

  const action = useMutation({
    mutationFn: ({ run }: { run: () => Promise<unknown>; message: string }) => run(),
    onSuccess: async (_r, { message }) => {
      setDialog(null);
      toast.success(`${q.quotationNo}: ${message}`);
      await queryClient.invalidateQueries({ queryKey: ['quotation', q.id] });
      await queryClient.invalidateQueries({ queryKey: ['quotations'] });
    },
  });
  const perform = (run: () => Promise<unknown>, message: string) => action.mutate({ run, message });

  return (
    <>
      {maintain && canIterate(q.status) && (
        <Button
          variant="secondary"
          icon={<Layers size={16} />}
          onClick={() => setDialog('iterate')}
        >
          New Iteration
        </Button>
      )}
      {allowed.submit && (
        <Button
          variant="accent"
          icon={<Send size={16} />}
          busy={action.isPending}
          onClick={() => perform(() => underwritingApi.submitQuotation(q.id), 'submitted')}
        >
          Submit
        </Button>
      )}
      {allowed.approve && (
        <Button
          variant="accent"
          icon={<CheckCircle2 size={16} />}
          busy={action.isPending}
          onClick={() => perform(() => underwritingApi.approveQuotation(q.id), 'approved')}
        >
          Approve
        </Button>
      )}
      {allowed.reject && (
        <Button variant="danger" icon={<XCircle size={16} />} onClick={() => setDialog('reject')}>
          Reject
        </Button>
      )}
      {maintain && q.status === 'APPROVED' && (
        <Button
          variant="accent"
          icon={<FileOutput size={16} />}
          onClick={() => setDialog('convert')}
        >
          Convert to Policy
        </Button>
      )}
      {dialog === null && <ErrorAlert error={action.error} />}
      <IterateDialog
        key={q.currentIteration}
        quotation={q}
        open={dialog === 'iterate'}
        onClose={() => setDialog(null)}
        busy={action.isPending}
        error={action.error}
        onSave={(value) =>
          perform(() => underwritingApi.iterateQuotation(q.id, value), 'new iteration saved')
        }
      />
      <DecisionDialog
        mode={dialog === 'reject' ? 'reject' : null}
        label={q.quotationNo}
        busy={action.isPending}
        error={action.error}
        onClose={() => setDialog(null)}
        onApprove={() => perform(() => underwritingApi.approveQuotation(q.id), 'approved')}
        onReject={(reason) =>
          perform(() => underwritingApi.rejectQuotation(q.id, reason), 'rejected')
        }
      />
      <ConvertDialog quotation={q} open={dialog === 'convert'} onClose={() => setDialog(null)} />
    </>
  );
}
