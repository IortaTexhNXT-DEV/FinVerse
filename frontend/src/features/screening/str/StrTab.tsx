import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, FileSignature, Save } from 'lucide-react';
import { useState } from 'react';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { ExportButtons } from '@/features/reports/ExportButtons';
import type { CaseDetail } from '../cases/api';
import { strApi } from './api';
import type { StrDetail } from './api';
import { FilingDialog } from './StrDialogs';
import { ReasonCodes, TemplateFields, Transactions } from './StrEditor';
import { keyed, plain, transactionsError } from './strLogic';
import type { EditLine } from './strLogic';

function StrForm({ detail, str }: Readonly<{ detail: CaseDetail; str: StrDetail }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const download = useFileDownload();
  const [values, setValues] = useState(str.values);
  const [reasons, setReasons] = useState(str.str.reasonCodes);
  const [lines, setLines] = useState<EditLine[]>(() => keyed(str.transactions));
  const [filing, setFiling] = useState(false);
  const editable = detail.actions.includes('STR_EDIT') && str.str.status === 'DRAFT';
  const refresh = () =>
    queryClient.invalidateQueries({ queryKey: ['screening', 'case', detail.row.id] });
  const save = useMutation({
    mutationFn: () =>
      strApi.save(str.str.id, { values, reasonCodes: reasons, transactions: plain(lines) }),
    onSuccess: async () => {
      toast.success(`${str.str.strNo} saved`);
      await refresh();
    },
  });
  const ready = useMutation({
    mutationFn: async () => {
      await strApi.save(str.str.id, { values, reasonCodes: reasons, transactions: plain(lines) });
      return strApi.ready(str.str.id);
    },
    onSuccess: async (result) => {
      toast.success(
        `${result.str.strNo} is ${result.str.status}; the case moves to STR extraction`,
      );
      await refresh();
    },
  });
  const lineError = transactionsError(plain(lines));
  return (
    <Card
      title={`STR ${str.str.strNo}`}
      actions={
        <>
          <StatusBadge status={str.str.status} />
          <ExportButtons
            formats={['PDF', 'DOCX']}
            onExport={(format) =>
              download.mutate(() => strApi.document(str.str.id, format === 'DOCX' ? 'DOCX' : 'PDF'))
            }
          />
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error ?? ready.error ?? download.error} />
        <section aria-label="Subject party">
          <h3>Subject Party (snapshot)</h3>
          <pre className="muted">{str.subjectSnapshot}</pre>
        </section>
        <TemplateFields
          fields={str.fields}
          values={values}
          gaps={str.gaps}
          disabled={!editable}
          onChange={setValues}
        />
        <ReasonCodes
          value={reasons}
          disabled={!editable}
          error={str.gaps.reasonCodes}
          onChange={setReasons}
        />
        <h3>Transactions</h3>
        <Transactions
          lines={lines}
          disabled={!editable}
          error={lineError ?? str.gaps.transactions}
          onChange={setLines}
        />
        <div className="row">
          {editable && (
            <>
              <Button
                variant="secondary"
                icon={<Save size={16} />}
                busy={save.isPending}
                disabled={lineError !== undefined}
                onClick={() => save.mutate()}
              >
                Save
              </Button>
              <Button
                variant="accent"
                icon={<CheckCircle2 size={16} />}
                busy={ready.isPending}
                disabled={lineError !== undefined}
                onClick={() => ready.mutate()}
              >
                Mark Ready
              </Button>
            </>
          )}
          {detail.actions.includes('FILING') && str.str.status === 'EXTRACTED' && (
            <Button icon={<FileSignature size={16} />} onClick={() => setFiling(true)}>
              Record Filing
            </Button>
          )}
        </div>
      </div>
      {filing && (
        <FilingDialog
          str={str.str}
          onClose={() => setFiling(false)}
          onDone={() => {
            setFiling(false);
            void refresh();
          }}
        />
      )}
    </Card>
  );
}

/**
 * The STR tab (SNSRP-705, 706; FR-SS-070, 072): Prepare STR in STR preparation, then the prefilled
 * STR with its completeness gaps, Save and Mark Ready while it is a draft, the PDF / Word copy and
 * Record Filing once it is extracted.
 */
export function StrTab({ detail }: Readonly<{ detail: CaseDetail }>) {
  const queryClient = useQueryClient();
  const str = useQuery({
    queryKey: ['screening', 'case', detail.row.id, 'str'],
    queryFn: () => strApi.ofCase(detail.row.id),
  });
  const prepare = useMutation({
    mutationFn: () => strApi.prepare(detail.row.id),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['screening', 'case', detail.row.id] }),
  });
  if (str.isLoading) {
    return <span className="spinner" aria-label="Loading" />;
  }
  if (str.data === undefined) {
    const canPrepare = detail.actions.includes('STR_EDIT');
    return (
      <Card>
        <ErrorAlert error={str.error ?? prepare.error} />
        <EmptyState
          message={
            canPrepare
              ? 'Prepare the STR: the subject, narrative and transactions are prefilled from the case.'
              : 'No STR has been prepared for this case.'
          }
        />
        {canPrepare && (
          <Button variant="accent" busy={prepare.isPending} onClick={() => prepare.mutate()}>
            Prepare STR
          </Button>
        )}
      </Card>
    );
  }
  return <StrForm key={str.dataUpdatedAt} detail={detail} str={str.data} />;
}
