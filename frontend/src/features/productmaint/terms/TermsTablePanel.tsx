import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileDown } from 'lucide-react';
import { useState } from 'react';
import { saveFile } from '@/api/client';
import { pmTermsApi } from '@/api/pmTerms';
import type { OptionColumn, TermsRecordType, TermsTable, TermsView } from '@/api/pmTerms';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { LoadingPanel } from '@/components/ui/LoadingPanel';
import { useToast } from '@/components/ui/toastContext';
import { FieldsDialog } from './FieldsDialog';
import { HistoryDialog } from './HistoryDialog';
import { OptionDialog } from './OptionDialog';
import { ProposalFilesCard } from './ProposalFilesCard';
import { TermsGrid } from './TermsGrid';
import { changedTerms, selectableInsurers } from './termsTable';

interface TermsTablePanelProps {
  type: TermsRecordType;
  id: number;
  /** Permission that keys in options and generates the proposal. */
  keyInPermission: string;
  /** Permissions that select the insurers and edit the Final Terms. */
  selectPermissions: readonly string[];
  /** Whether the insurers can be selected now (the record's stage). */
  selectable: boolean;
  /** Whether the proposal can be generated now. */
  generatable: boolean;
  onChanged?: () => void;
}

type Editing = { column: OptionColumn; optionNo: number | null } | null;

function useTerms(type: TermsRecordType, id: number) {
  const queryClient = useQueryClient();
  const key = ['pm-terms', type, id];
  const view = useQuery({ queryKey: key, queryFn: () => pmTermsApi.table(type, id) });
  const files = useQuery({
    queryKey: [...key, 'files'],
    queryFn: () => pmTermsApi.files(type, id),
  });
  const update = (next: TermsView) => queryClient.setQueryData(key, next);
  const refresh = () => queryClient.invalidateQueries({ queryKey: key });
  return { view, files, update, refresh };
}

function ExportButtons({ type, id }: Readonly<{ type: TermsRecordType; id: number }>) {
  const exportFile = useMutation({
    mutationFn: (format: 'xlsx' | 'pdf') => pmTermsApi.exportTable(type, id, format),
    onSuccess: (file) => saveFile(file.blob, file.fileName),
  });
  return (
    <>
      {(['xlsx', 'pdf'] as const).map((format) => (
        <Button
          key={format}
          variant="secondary"
          icon={<FileDown size={16} />}
          busy={exportFile.isPending && exportFile.variables === format}
          onClick={() => exportFile.mutate(format)}
        >
          {format === 'xlsx' ? 'Excel' : 'PDF'}
        </Button>
      ))}
    </>
  );
}

interface SelectionProps {
  type: TermsRecordType;
  id: number;
  table: TermsTable;
  onSaved: (view: TermsView) => void;
}

/** Insurer selection and Proceed to Proposal (BDOI FRS FRPM.008.01). */
function InsurerSelection({ type, id, table, onSaved }: Readonly<SelectionProps>) {
  const toast = useToast();
  const [chosen, setChosen] = useState<string[]>(table.selectedInsurers);
  const proceed = useMutation({
    mutationFn: () => pmTermsApi.proceed(type, id, chosen),
    onSuccess: (saved) => {
      onSaved(saved);
      toast.success('Insurers selected; the proposal can be generated');
    },
  });
  return (
    <Card title="Insurers for the Proposal">
      <ErrorAlert error={proceed.error} />
      <fieldset className="field">
        <legend>Select one or more insurers that gave terms</legend>
        <div className="insurer-choices">
          {selectableInsurers(table).map((i) => (
            <label key={i.code} className="checkbox">
              <input
                type="checkbox"
                checked={chosen.includes(i.code)}
                onChange={(e) =>
                  setChosen(
                    e.target.checked ? [...chosen, i.code] : chosen.filter((c) => c !== i.code),
                  )
                }
              />
              {i.name}
            </label>
          ))}
        </div>
      </fieldset>
      <Button
        disabled={chosen.length === 0}
        busy={proceed.isPending}
        onClick={() => proceed.mutate()}
      >
        Proceed to Proposal
      </Button>
    </Card>
  );
}

interface TermsCardProps {
  type: TermsRecordType;
  id: number;
  view: TermsView;
  keyIn: boolean;
  select: boolean;
  onSaved: (view: TermsView) => void;
}

/** The comparative table with its tools and the Final Terms to save. */
function TermsCard({ type, id, view, keyIn, select, onSaved }: Readonly<TermsCardProps>) {
  const toast = useToast();
  const [edited, setEdited] = useState<Record<string, string>>({});
  const [editing, setEditing] = useState<Editing>(null);
  const [dialog, setDialog] = useState<'fields' | 'history' | null>(null);
  const pending = changedTerms(view.table.finalTerms, edited);
  const saveTerms = useMutation({
    mutationFn: () => pmTermsApi.saveFinalTerms(type, id, pending),
    onSuccess: (saved) => {
      onSaved(saved);
      setEdited({});
      toast.success('Final Terms for Proposal saved');
    },
  });
  const toolbar = (
    <>
      {keyIn && (
        <Button variant="secondary" onClick={() => setDialog('fields')}>
          Choose Fields
        </Button>
      )}
      <Button variant="secondary" onClick={() => setDialog('history')}>
        Final Terms History
      </Button>
      <ExportButtons type={type} id={id} />
    </>
  );
  return (
    <Card title="Comparative Table" actions={toolbar} flush>
      <ErrorAlert error={saveTerms.error} />
      <TermsGrid
        view={view}
        editable={select}
        edited={edited}
        onEdit={(key, value) => setEdited({ ...edited, [key]: value })}
        onOption={keyIn ? (column, optionNo) => setEditing({ column, optionNo }) : undefined}
      />
      {select && Object.keys(pending).length > 0 && (
        <div className="card-pad">
          <Button busy={saveTerms.isPending} onClick={() => saveTerms.mutate()}>
            Save Final Terms
          </Button>
        </div>
      )}
      {editing && (
        <OptionDialog
          type={type}
          id={id}
          view={view}
          column={editing.column}
          optionNo={editing.optionNo}
          onSaved={onSaved}
          onClose={() => setEditing(null)}
        />
      )}
      {dialog === 'fields' && (
        <FieldsDialog
          type={type}
          id={id}
          view={view}
          onSaved={onSaved}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog === 'history' && (
        <HistoryDialog type={type} id={id} onClose={() => setDialog(null)} />
      )}
    </Card>
  );
}

/**
 * The comparative table of BDOI's FRS for a quotation request or a package request: the fields
 * chosen as rows with the QS value, one column per quotation option of each insurer with its
 * response, the Final Terms for Proposal (edited values kept in the history), the insurer
 * selection with Proceed to Proposal, Generate Proposal per selected insurer and the exports.
 */
export function TermsTablePanel({
  type,
  id,
  keyInPermission,
  selectPermissions,
  selectable,
  generatable,
  onChanged,
}: Readonly<TermsTablePanelProps>) {
  const { can } = useAuth();
  const toast = useToast();
  const { view, files, update, refresh } = useTerms(type, id);
  const keyIn = can(keyInPermission);
  const select = selectPermissions.some((p) => can(p));
  const saved = (next: TermsView) => {
    update(next);
    onChanged?.();
  };
  const generate = useMutation({
    mutationFn: () => pmTermsApi.generate(type, id),
    onSuccess: async (made) => {
      toast.success(`${made.length} proposal slip(s) generated`);
      await refresh();
      onChanged?.();
    },
  });
  if (!view.data) {
    return view.isLoading ? <LoadingPanel /> : <ErrorAlert error={view.error} />;
  }
  const data = view.data;
  const canGenerate = keyIn && generatable && data.table.selectedInsurers.length > 0;
  return (
    <div className="stack">
      <TermsCard type={type} id={id} view={data} keyIn={keyIn} select={select} onSaved={saved} />
      {select && selectable && (
        <InsurerSelection type={type} id={id} table={data.table} onSaved={saved} />
      )}
      <ErrorAlert error={generate.error} />
      <ProposalFilesCard
        files={files.data ?? []}
        loading={files.isLoading}
        actions={
          canGenerate ? (
            <Button busy={generate.isPending} onClick={() => generate.mutate()}>
              Generate Proposal
            </Button>
          ) : undefined
        }
      />
    </div>
  );
}
