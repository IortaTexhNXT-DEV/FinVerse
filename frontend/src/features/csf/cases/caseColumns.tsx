import { Link } from 'react-router-dom';
import type { CaseRow } from '@/api/csfCases';
import type { Column } from '@/components/ui/DataTable';
import { formatDate, formatDateTime } from '@/utils/format';
import { SlaTags } from './SlaTags';

/** Columns of the case lists. */
export const CASE_COLUMNS: Column<CaseRow>[] = [
  {
    key: 'caseNo',
    header: 'Case ID',
    render: (c) => <Link to={`/csf/cases/${String(c.id)}`}>{c.caseNo}</Link>,
  },
  { key: 'created', header: 'Created', render: (c) => formatDateTime(c.createdAt) },
  { key: 'client', header: 'Client / Caller', render: (c) => c.client, truncate: true },
  { key: 'type', header: 'Case Type', render: (c) => c.caseType.label },
  { key: 'nature', header: 'Nature of Transaction', render: (c) => c.nature.label, truncate: true },
  { key: 'unit', header: 'Fulfilment Unit', render: (c) => c.fulfilmentUnit.label },
  { key: 'owner', header: 'Owner', render: (c) => c.owner },
  { key: 'status', header: 'Status', render: (c) => c.statusLabel },
  { key: 'due', header: 'Due Date', render: (c) => formatDate(c.dueDate) },
  { key: 'age', header: 'Age', numeric: true, render: (c) => c.sla.ageDays },
  { key: 'sla', header: 'SLA', render: (c) => <SlaTags row={c} /> },
];
