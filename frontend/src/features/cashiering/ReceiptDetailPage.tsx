import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';
import { cashieringApi } from './cashieringApi';
import type { ReceiptAction, ReceiptDetail, ReinstateBody } from './cashieringApi';
import { RECEIPT_TABS } from './cashieringLogic';
import type { ReceiptTabId } from './cashieringLogic';
import { ReceiptTabContent } from './ReceiptTabs';
import { ReceiptHeaderActions, ReceiptSummaryCard } from './ReceiptParts';
import { ReinstateDialog } from './ReinstateDialog';

const ACTION_ENTITY = 'ReceiptAction';

function latestAction(r: ReceiptDetail): ReceiptAction | undefined {
  return r.actions.length === 0 ? undefined : r.actions[r.actions.length - 1];
}

/**
 * Receipt page (CSHID.010-015): the AR or OR with its status, the approval of an open
 * cancellation or reinstatement (workflow OPS_RECEIPT_ACTION), and its applications, lines,
 * journals and history. Print re-issues the PDF; Cancel and Reinstate go to the checker.
 */
export default function ReceiptDetailPage() {
  const id = Number(useParams().id);
  const toast = useToast();
  const queryClient = useQueryClient();
  const download = useFileDownload();
  const [tab, setTab] = useState<ReceiptTabId>('applications');
  const [dialog, setDialog] = useState<'cancel' | 'reinstate'>();
  const receipt = useQuery({
    queryKey: ['cashiering', 'receipt', id],
    queryFn: () => cashieringApi.receipt(id),
    enabled: id > 0,
  });
  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['cashiering'] });
  };
  const act = useMutation({
    mutationFn: (fn: () => Promise<ReceiptAction>) => fn(),
    onSuccess: async (a) => {
      setDialog(undefined);
      toast.success(`${a.transactionNo} is ${humanize(a.stage).toLowerCase()}`);
      await refresh();
    },
  });
  const r = receipt.data;
  if (r === undefined) {
    return <ErrorAlert error={receipt.error} />;
  }
  const s = r.summary;
  const action = latestAction(r);
  const open =
    action !== undefined && (action.stage === 'REQUESTED' || action.stage === 'FOR_APPROVAL');
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title={`${s.kind === 'AR' ? 'Acknowledgement Receipt' : 'Official Receipt'} ${s.receiptNo}`}
        description={`${humanize(s.receiptClass)} receipt from ${[humanize(s.source), r.sourceRef].filter(Boolean).join(' ')}.`}
        backTo="/cashiering/receipts"
        actions={
          <ReceiptHeaderActions
            receipt={r}
            requestOpen={open}
            printing={download.isPending}
            onPrint={() => download.mutate(() => cashieringApi.receiptPdf(id))}
            onCancel={() => setDialog('cancel')}
            onReinstate={() => setDialog('reinstate')}
          />
        }
      />
      <ErrorAlert error={download.error ?? act.error} />
      <ReceiptSummaryCard receipt={r} />
      {action !== undefined && (
        <WorkflowPanel
          entityType={ACTION_ENTITY}
          entityId={action.id}
          onChanged={() => void refresh()}
          renderBusinessActions={(actions) =>
            actions.map((a) => (
              <Button
                key={a.action}
                size="sm"
                variant={a.action === 'approve' ? 'accent' : 'secondary'}
                onClick={() =>
                  act.mutate(() =>
                    a.action === 'approve'
                      ? cashieringApi.approveAction(action.id)
                      : cashieringApi.resubmitAction(action.id),
                  )
                }
              >
                {a.label}
              </Button>
            ))
          }
        />
      )}
      <Card flush>
        <Tabs tabs={RECEIPT_TABS} active={tab} onChange={setTab} />
        <ReceiptTabContent tab={tab} receipt={r} />
      </Card>
      {dialog === 'cancel' && (
        <ActionDialog
          title={`Cancel ${s.receiptNo}`}
          reasonLov="RECEIPT_CANCEL_REASON"
          confirmLabel="Submit Cancellation"
          busy={act.isPending}
          error={act.error}
          onConfirm={(note) =>
            act.mutate(() => cashieringApi.cancel(id, note.reasonCode ?? '', note.comment))
          }
          onClose={() => setDialog(undefined)}
        />
      )}
      {dialog === 'reinstate' && (
        <ReinstateDialog
          receipt={s}
          busy={act.isPending}
          error={act.error}
          onSubmit={(body: ReinstateBody) => act.mutate(() => cashieringApi.reinstate(id, body))}
          onClose={() => setDialog(undefined)}
        />
      )}
    </div>
  );
}
