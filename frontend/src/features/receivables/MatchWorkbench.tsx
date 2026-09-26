import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link2, Unlink, Wand2 } from 'lucide-react';
import { useState } from 'react';
import { receivablesApi } from '@/api/receivables';
import type { BookEntry, Match, StatementLine } from '@/api/receivables';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDate } from '@/utils/format';
import { selectionBalance } from './receivablesMath';

interface WorkbenchProps {
  companyId: number;
  bank: string;
  asOf: string;
}

function toggle(list: number[], id: number): number[] {
  return list.includes(id) ? list.filter((x) => x !== id) : [...list, id];
}

/**
 * Matching workbench: unmatched book entries and bank statement lines side by side, automatic
 * matching (amount, date window, reference, deposit slips) and manual matching of a balanced
 * selection; recent matches can be undone.
 */
export function MatchWorkbench({ companyId, bank, asOf }: Readonly<WorkbenchProps>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [bookIds, setBookIds] = useState<number[]>([]);
  const [lineIds, setLineIds] = useState<number[]>([]);
  const [windowDays, setWindowDays] = useState(7);
  const workbench = useQuery({
    queryKey: ['workbench', companyId, bank, asOf],
    queryFn: () => receivablesApi.workbench(companyId, bank, asOf),
  });
  const matches = useQuery({
    queryKey: ['matches', companyId, bank],
    queryFn: () => receivablesApi.matches(companyId, bank),
  });
  const action = useMutation({
    mutationFn: (run: () => Promise<string>) => run(),
    onSuccess: async (message) => {
      setBookIds([]);
      setLineIds([]);
      await queryClient.invalidateQueries({ queryKey: ['workbench'] });
      await queryClient.invalidateQueries({ queryKey: ['matches'] });
      await queryClient.invalidateQueries({ queryKey: ['brs'] });
      toast.success(message);
    },
  });
  const book = workbench.data?.bookEntries ?? [];
  const lines = workbench.data?.bankLines ?? [];
  const selection = selectionBalance(
    book.filter((e) => bookIds.includes(e.id)),
    lines.filter((l) => lineIds.includes(l.id)),
  );

  return (
    <div className="stack">
      <ErrorAlert error={action.error ?? workbench.error} />
      <Card>
        <div className="row">
          <Field label="Date window (days)">
            {(id) => (
              <input
                id={id}
                type="number"
                min={0}
                max={60}
                className="input num"
                value={windowDays}
                onChange={(e) => setWindowDays(Number(e.target.value))}
              />
            )}
          </Field>
          <Button
            variant="accent"
            icon={<Wand2 size={16} />}
            busy={action.isPending}
            onClick={() =>
              action.mutate(async () => {
                const created = await receivablesApi.autoMatch(companyId, bank, asOf, windowDays);
                return `${created.length} matches created`;
              })
            }
          >
            Auto-match
          </Button>
          <div className="spacer" />
          <span>
            Book {formatAmount(selection.bookTotal)} · Bank {formatAmount(selection.bankTotal)} ·
            Difference <strong>{formatAmount(selection.difference)}</strong>
          </span>
          <Button
            icon={<Link2 size={16} />}
            disabled={!selection.balanced}
            onClick={() =>
              action.mutate(async () => {
                await receivablesApi.manualMatch(companyId, bank, bookIds, lineIds);
                return 'Items matched';
              })
            }
          >
            Match Selected
          </Button>
        </div>
      </Card>
      <div className="grid-2">
        <Card title={`Book entries not reconciled (${book.length})`} flush>
          <DataTable<BookEntry>
            loading={workbench.isLoading}
            rows={book}
            rowKey={(e) => e.id}
            caption="Book entries"
            columns={[
              {
                key: 'sel',
                header: 'Select',
                render: (e) => (
                  <input
                    type="checkbox"
                    aria-label={`Select book entry ${e.batchNo}`}
                    checked={bookIds.includes(e.id)}
                    onChange={() => setBookIds((s) => toggle(s, e.id))}
                  />
                ),
              },
              { key: 'date', header: 'Date', render: (e) => formatDate(e.valueDate) },
              { key: 'ref', header: 'Reference', render: (e) => e.reference ?? e.batchNo },
              { key: 'nar', header: 'Narration', render: (e) => e.narration ?? '' },
              {
                key: 'dr',
                header: 'Debit',
                numeric: true,
                render: (e) => <Amount value={e.debit || null} />,
              },
              {
                key: 'cr',
                header: 'Credit',
                numeric: true,
                render: (e) => <Amount value={e.credit || null} />,
              },
            ]}
          />
        </Card>
        <Card title={`Bank lines not reconciled (${lines.length})`} flush>
          <DataTable<StatementLine>
            loading={workbench.isLoading}
            rows={lines}
            rowKey={(l) => l.id}
            caption="Bank statement lines"
            columns={[
              {
                key: 'sel',
                header: 'Select',
                render: (l) => (
                  <input
                    type="checkbox"
                    aria-label={`Select bank line ${l.lineNo}`}
                    checked={lineIds.includes(l.id)}
                    onChange={() => setLineIds((s) => toggle(s, l.id))}
                  />
                ),
              },
              { key: 'date', header: 'Date', render: (l) => formatDate(l.valueDate) },
              { key: 'ref', header: 'Reference', render: (l) => l.reference ?? '' },
              { key: 'desc', header: 'Description', render: (l) => l.description ?? '' },
              {
                key: 'cr',
                header: 'Deposit',
                numeric: true,
                render: (l) => <Amount value={l.credit || null} />,
              },
              {
                key: 'dr',
                header: 'Withdrawal',
                numeric: true,
                render: (l) => <Amount value={l.debit || null} />,
              },
            ]}
          />
        </Card>
      </div>
      <Card title="Recent matches" flush>
        <DataTable<Match>
          loading={matches.isLoading}
          rows={(matches.data ?? []).slice(0, 20)}
          rowKey={(m) => m.id}
          caption="Matches"
          columns={[
            { key: 'id', header: 'Match', render: (m) => `#${m.id}` },
            { key: 'method', header: 'Method', render: (m) => m.method },
            { key: 'date', header: 'Date', render: (m) => formatDate(m.matchDate) },
            {
              key: 'amt',
              header: 'Amount',
              numeric: true,
              render: (m) => <Amount value={m.amount} />,
            },
            { key: 'by', header: 'By', render: (m) => m.createdBy },
            {
              key: 'undo',
              header: 'Undo',
              render: (m) => (
                <Button
                  size="sm"
                  variant="ghost"
                  icon={<Unlink size={14} />}
                  onClick={() =>
                    action.mutate(async () => {
                      await receivablesApi.unmatch(m.id);
                      return `Match #${m.id} undone`;
                    })
                  }
                >
                  Unmatch
                </Button>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
