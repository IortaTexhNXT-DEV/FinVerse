import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FilePlus2 } from 'lucide-react';
import { useState } from 'react';
import type { ReactNode } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { today } from '@/utils/format';
import { screeningSetupApi } from './api';
import type { ConfigContent, ConfigType, ConfigVersion, ConfigVersionDetail } from './api';
import { defaultVersion, keyed, mapTables, unkeyed, versionRoles } from './configSpec';
import { RemarksDialog } from './RemarksDialog';
import { ChangesCard, VersionActions, VersionHeaderCard, VersionList } from './VersionParts';
import type { Draft } from './VersionParts';

type RenderRows = (
  content: ConfigContent,
  editable: boolean,
  onChange: (content: ConfigContent) => void,
) => ReactNode;

function initialDraft(detail: ConfigVersionDetail): Draft | undefined {
  const v = detail.version;
  return v.status === 'DRAFT'
    ? {
        id: v.id,
        effectiveFrom: v.effectiveFrom,
        changeNote: v.changeNote ?? '',
        content: mapTables(detail.content, keyed),
      }
    : undefined;
}

/** The selected version: header, rows, changes and the maker-checker actions. */
function VersionEditor({
  detail,
  renderRows,
}: Readonly<{ detail: ConfigVersionDetail; renderRows: RenderRows }>) {
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const version = detail.version;
  const roles = versionRoles(version, user?.username, can);
  const [draft, setDraft] = useState<Draft | undefined>(() =>
    roles.editable ? initialDraft(detail) : undefined,
  );
  const [rejecting, setRejecting] = useState(false);
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['screening-setup'] });
  const save = useMutation({
    mutationFn: (d: Draft) =>
      screeningSetupApi.saveDraft(d.id, {
        effectiveFrom: d.effectiveFrom,
        changeNote: d.changeNote,
        content: mapTables(d.content, unkeyed),
      }),
    onSuccess: async () => {
      await refresh();
      toast.success('Draft saved');
    },
  });
  const submit = useMutation({
    mutationFn: async (d: Draft) => {
      await save.mutateAsync(d);
      return screeningSetupApi.submit(d.id);
    },
    onSuccess: async (d) => {
      await refresh();
      toast.success(`${d.version.label} submitted for approval`);
    },
  });
  const decide = useMutation({
    mutationFn: (action: () => Promise<ConfigVersion>) => action(),
    onSuccess: async (v) => {
      setRejecting(false);
      await refresh();
      toast.success(`${v.label}: ${v.status.toLowerCase()}`);
    },
  });
  const busy = save.isPending || submit.isPending || decide.isPending;
  return (
    <>
      <ErrorAlert error={save.error ?? submit.error ?? decide.error} />
      <VersionHeaderCard
        version={version}
        draft={draft}
        onDraft={setDraft}
        actions={
          <VersionActions
            version={version}
            draft={draft}
            isMaker={roles.isMaker}
            mayDecide={roles.mayDecide}
            busy={busy}
            saving={save.isPending}
            submitting={submit.isPending}
            deciding={decide.isPending}
            onSave={(d) => save.mutate(d)}
            onSubmit={(d) => submit.mutate(d)}
            onDiscard={() => decide.mutate(() => screeningSetupApi.withdraw(version.id))}
            onApprove={() => decide.mutate(() => screeningSetupApi.approve(version.id))}
            onReject={() => setRejecting(true)}
          />
        }
      />
      {renderRows(draft?.content ?? detail.content, draft !== undefined, (c) => {
        if (draft !== undefined) {
          setDraft({ ...draft, content: c });
        }
      })}
      <ChangesCard changes={detail.changes} />
      {rejecting && (
        <RemarksDialog
          title={`Reject ${version.label}`}
          label="Reason for the rejection"
          confirmLabel="Reject"
          busy={decide.isPending}
          error={decide.error}
          onConfirm={(remarks) =>
            decide.mutate(() => screeningSetupApi.reject(version.id, remarks))
          }
          onClose={() => setRejecting(false)}
        />
      )}
    </>
  );
}

function VersionDetail({ id, renderRows }: Readonly<{ id: number; renderRows: RenderRows }>) {
  const detail = useQuery({
    queryKey: ['screening-setup', 'version', id],
    queryFn: () => screeningSetupApi.version(id),
  });
  if (detail.data === undefined) {
    return <ErrorAlert error={detail.error} />;
  }
  const v = detail.data.version;
  return <VersionEditor key={`${v.id}-${v.status}`} detail={detail.data} renderRows={renderRows} />;
}

interface WorkbenchProps {
  type: ConfigType;
  scope?: string;
  /** Version to open first (link from My Approvals or a notification). */
  initialVersion?: number;
  /** The rows of the version's type: editable for a draft. */
  renderRows: RenderRows;
}

/**
 * Versions of one configuration type (FR-SS-010, 019): the version list, the selected version with
 * its rows and changes, and the maker-checker actions (New Draft, Save, Submit for Approval,
 * Discard; Approve, Reject). The maker never sees Approve on the own version.
 */
export function VersionWorkbench({
  type,
  scope,
  initialVersion,
  renderRows,
}: Readonly<WorkbenchProps>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<number | undefined>(initialVersion);
  const versions = useQuery({
    queryKey: ['screening-setup', 'versions', companyId, type],
    queryFn: () => screeningSetupApi.versions(companyId, type),
    enabled: companyId > 0,
  });
  const own = (versions.data ?? []).filter((v) => (v.scope ?? undefined) === scope);
  const currentId = selected ?? defaultVersion(own, today(), scope)?.id;
  const newDraft = useMutation({
    mutationFn: () => screeningSetupApi.newDraft(companyId, type, scope),
    onSuccess: async (d) => {
      await queryClient.invalidateQueries({ queryKey: ['screening-setup'] });
      setSelected(d.version.id);
      toast.success(`Draft ${d.version.label} opened`);
    },
  });
  return (
    <div className="stack">
      <ErrorAlert error={versions.error ?? newDraft.error} />
      {can('SCR_CONFIG_MAINTAIN') && (
        <div className="row">
          <div className="spacer" />
          <Button
            icon={<FilePlus2 size={16} />}
            busy={newDraft.isPending}
            onClick={() => newDraft.mutate()}
          >
            New Draft
          </Button>
        </div>
      )}
      <VersionList versions={own} selected={currentId} onSelect={setSelected} />
      {currentId === undefined ? (
        <EmptyState message="Select a version or create a draft" />
      ) : (
        <VersionDetail key={currentId} id={currentId} renderRows={renderRows} />
      )}
    </div>
  );
}
