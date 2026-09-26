import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileBarChart2, Play } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { closingApi } from '@/api/closing';
import type { FxPreview, FxRun } from '@/api/closing';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { FxPreviewView, FxRunsTable } from './FxPreviewView';
import { PeriodSelectors } from './PeriodSelectors';
import { usePeriodPicker } from './usePeriodPicker';

function isPosted(preview: FxPreview | undefined): boolean {
  return preview?.existingRunId !== undefined;
}

function hasMissingRates(preview: FxPreview | undefined): boolean {
  return (preview?.missingRates.length ?? 0) > 0;
}

function postedMessage(r: FxRun): string {
  const journal = r.journalBatchNo ? ' as ' + r.journalBatchNo : ' (nothing to post)';
  return `Revaluation ${r.periodName} posted${journal}`;
}

/** Period-end FX revaluation: preview at the closing rate, post (once per period), history. */
export default function FxRevaluationPage() {
  const picker = usePeriodPicker();
  const { companyId, period } = picker;
  const periodId = period?.id ?? 0;
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [autoReverse, setAutoReverse] = useState(true);

  const preview = useQuery({
    queryKey: ['fx-preview', companyId, periodId],
    queryFn: () => closingApi.fxPreview(companyId, periodId),
    enabled: companyId > 0 && periodId > 0,
  });
  const runs = useQuery({
    queryKey: ['fx-runs', companyId],
    queryFn: () => closingApi.fxRuns(companyId),
    enabled: companyId > 0,
  });
  const post = useMutation({
    mutationFn: () => closingApi.fxPost(companyId, periodId, autoReverse),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['fx-runs'] });
      await queryClient.invalidateQueries({ queryKey: ['fx-preview'] });
      toast.success(postedMessage(r));
    },
  });
  const posted = isPosted(preview.data);
  const blocked = periodId === 0 || posted || hasMissingRates(preview.data);

  return (
    <div className="stack">
      <PageHeader
        section="Planning & Closing"
        title="FX Revaluation"
        description="Foreign currency balances of revaluation accounts are restated at the CLOSING rate; the difference is posted to unrealized FX gain/loss (4602). Each period is revalued once."
        actions={
          <Button
            variant="secondary"
            icon={<FileBarChart2 size={16} />}
            onClick={() => void navigate('/reports/GL-FXREV')}
          >
            Register
          </Button>
        }
      />
      <Card>
        <div className="form-grid">
          <PeriodSelectors picker={picker} />
          <label className="checkbox" style={{ alignSelf: 'end' }}>
            <input
              type="checkbox"
              checked={autoReverse}
              onChange={(e) => setAutoReverse(e.target.checked)}
            />
            Auto-reverse on the first day of the next period
          </label>
          <Button
            variant="accent"
            icon={<Play size={16} />}
            busy={post.isPending}
            disabled={blocked}
            onClick={() => post.mutate()}
            style={{ alignSelf: 'end' }}
          >
            {posted ? 'Already revalued' : 'Post revaluation'}
          </Button>
        </div>
      </Card>
      <ErrorAlert error={post.error ?? preview.error} />
      <FxPreviewView preview={preview.data} loading={preview.isLoading} />
      <FxRunsTable runs={runs.data ?? []} loading={runs.isLoading} />
    </div>
  );
}
