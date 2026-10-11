import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ChevronsDownUp, ChevronsUpDown, FileDown, Plus, User } from 'lucide-react';
import { useMemo, useState } from 'react';
import { catalogApi } from '@/api/catalog';
import type { SalesOrganisation } from '@/api/catalog';
import { saveFile } from '@/api/client';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { TreeTable } from '@/components/ui/TreeTable';
import { branchKeys } from '@/components/ui/treeRows';
import { useDisplayName } from '@/components/ui/useDisplayName';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { AddModal, UnitDetailModal } from './SalesOrgDialogs';
import { ReasonDialog } from './SalesOrgReasonDialog';
import { orgColumns, statusOf, summaryText, useRowActions } from './salesOrgColumns';
import { BLANK_FORM, orgView, salesTree, unitKey } from './salesTree';
import type { AddForm, OrgRow, ReasonAction, SalesNode } from './salesTree';

const SALES = ['catalog', 'sales'] as const;

/** Regions expanded when the page opens, so the departments are in sight. */
function initialExpanded(org: SalesOrganisation | undefined): Set<string> {
  return new Set((org?.units ?? []).filter((u) => u.level !== 'TEAM').map((u) => unitKey(u.code)));
}

/** Authorize a unit or an officer, and the Excel export. */
function useOrgMutations(companyId: number) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const nameOf = useDisplayName();
  const authorize = useMutation({
    mutationFn: (row: OrgRow) =>
      row.kind === 'officer'
        ? catalogApi.authorize('SALES_OFFICER', row.officer.id)
        : catalogApi.authorize('SALES_UNIT', row.node.unit.id),
    onSuccess: async (r, row) => {
      await queryClient.invalidateQueries({ queryKey: SALES });
      toast.success(
        `${row.kind === 'officer' ? nameOf(row.officer.username) : r.reference} authorized`,
      );
    },
    onError: (e) => toast.error(e.message),
  });
  const download = useMutation({
    mutationFn: () => catalogApi.exportSalesOrganisation(companyId),
    onSuccess: (file) => {
      saveFile(file.blob, file.fileName);
      toast.success(`${file.fileName} downloaded`);
    },
  });
  return { authorize, download };
}

interface OrgToolbarProps {
  onSearch: (text: string) => void;
  onExpandAll: () => void;
  onCollapseAll: () => void;
  showInactive: boolean;
  onShowInactive: (show: boolean) => void;
  exporting: boolean;
  onExport: () => void;
}

/** Search, Expand All / Collapse All, Show Inactive and Export to Excel. */
function OrgToolbar(props: Readonly<OrgToolbarProps>) {
  return (
    <WorklistToolbar
      placeholder="Search unit code, name or officer"
      onSearch={props.onSearch}
      extra={
        <>
          <Button variant="ghost" icon={<ChevronsUpDown size={16} />} onClick={props.onExpandAll}>
            Expand All
          </Button>
          <Button variant="ghost" icon={<ChevronsDownUp size={16} />} onClick={props.onCollapseAll}>
            Collapse All
          </Button>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={props.showInactive}
              onChange={(e) => props.onShowInactive(e.target.checked)}
            />
            Show Inactive
          </label>
        </>
      }
    >
      <Button
        variant="secondary"
        icon={<FileDown size={16} />}
        busy={props.exporting}
        onClick={props.onExport}
      >
        Export to Excel
      </Button>
    </WorklistToolbar>
  );
}

interface OrgDialogsProps {
  org: SalesOrganisation | undefined;
  maintain: boolean;
  adding: AddForm | null;
  viewing: SalesNode | null;
  reasonAction: ReasonAction | null;
  onClose: () => void;
}

/** The open dialog of the page: add, unit details, or an action with a reason. */
function OrgDialogs({
  org,
  maintain,
  adding,
  viewing,
  reasonAction,
  onClose,
}: Readonly<OrgDialogsProps>) {
  return (
    <>
      {adding && <AddModal org={org} initial={adding} onClose={onClose} />}
      {viewing && <UnitDetailModal node={viewing} org={org} canEdit={maintain} onClose={onClose} />}
      {reasonAction && <ReasonDialog action={reasonAction} onClose={onClose} />}
    </>
  );
}

/**
 * Sales organisation (BRNB.010/075/108): regions, departments and teams with their cost centers,
 * and the account officers of each team, as one tree table. Accounts are stamped with the
 * creator's units and cost center when they are created.
 */
export default function SalesOrganisationPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const nameOf = useDisplayName();
  const [adding, setAdding] = useState<AddForm | null>(null);
  const [viewing, setViewing] = useState<SalesNode | null>(null);
  const [reasonAction, setReasonAction] = useState<ReasonAction | null>(null);
  const [search, setSearch] = useState('');
  const [showInactive, setShowInactive] = useState(false);
  const [expanded, setExpanded] = useState<Set<string> | null>(null);
  const org = useQuery({
    queryKey: [...SALES, companyId],
    queryFn: () => catalogApi.salesOrganisation(companyId),
  });
  const tree = useMemo(() => (org.data ? salesTree(org.data) : []), [org.data]);
  const view = orgView(tree, { showInactive, search, nameOf });
  const opened = expanded ?? initialExpanded(org.data);
  const searching = search.trim() !== '';
  const shownExpanded = searching ? new Set([...opened, ...view.matchPath]) : opened;
  const { authorize, download } = useOrgMutations(companyId);
  const actionsOf = useRowActions({
    view: setViewing,
    add: setAdding,
    reason: setReasonAction,
    authorize: (row) => authorize.mutate(row),
  });
  const toggle = (key: string) => {
    const next = new Set(shownExpanded);
    if (next.has(key)) {
      next.delete(key);
    } else {
      next.add(key);
    }
    setExpanded(next);
  };
  const maintain = can('MASTER_MAINTAIN');
  const openUnit = (row: OrgRow) => {
    if (row.kind === 'unit') {
      setViewing(row.node);
    }
  };
  const newUnit = (
    <Button variant="accent" icon={<Plus size={16} />} onClick={() => setAdding(BLANK_FORM)}>
      New Unit
    </Button>
  );
  const emptyAction = maintain && !searching ? newUnit : undefined;
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Sales Organisation"
        description="Regions, departments and teams, their cost centers and the account officers of each team."
        actions={
          maintain && (
            <>
              <Button
                variant="secondary"
                icon={<User size={16} />}
                onClick={() => setAdding({ ...BLANK_FORM, mode: 'officer' })}
              >
                Assign Officer
              </Button>
              {newUnit}
            </>
          )
        }
      />
      <ErrorAlert error={org.error ?? download.error} />
      <Card flush callout="sales-organisation">
        <OrgToolbar
          onSearch={setSearch}
          onExpandAll={() => setExpanded(new Set(branchKeys(view.nodes)))}
          onCollapseAll={() => setExpanded(new Set())}
          showInactive={showInactive}
          onShowInactive={setShowInactive}
          exporting={download.isPending}
          onExport={() => download.mutate()}
        />
        {!org.isLoading && view.nodes.length > 0 && (
          <p className="tree-summary" aria-live="polite">
            {summaryText(view.counts)}
          </p>
        )}
        <TreeTable
          caption="Sales organisation"
          callout="sales-organisation-tree"
          columns={orgColumns(actionsOf)}
          nodes={view.nodes}
          expanded={shownExpanded}
          onToggle={toggle}
          onActivate={openUnit}
          loading={org.isLoading}
          emptyMessage={
            searching ? 'No unit or officer matches the search' : 'No sales unit defined yet'
          }
          emptyAction={emptyAction}
          rowClassName={(r) => (statusOf(r) === 'INACTIVE' ? 'row-inactive' : undefined)}
        />
      </Card>
      <OrgDialogs
        org={org.data}
        maintain={maintain}
        adding={adding}
        viewing={viewing}
        reasonAction={reasonAction}
        onClose={() => {
          setAdding(null);
          setViewing(null);
          setReasonAction(null);
        }}
      />
    </div>
  );
}
