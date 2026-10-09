import { Link } from 'react-router-dom';
import type { AlertItem } from '@/api/alerts';
import type { Definition } from '@/components/ui/DefinitionGrid';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/utils/format';
import { recordLink, recordText } from './alertWording';
import { SeverityBadge } from './SeverityBadge';

/** The key facts of an alert: rule, severity, raised on, record and status. */
export function alertFacts(alert: AlertItem, rule: string): Definition[] {
  const link = recordLink(alert);
  const record = recordText(alert);
  return [
    { label: 'Rule', value: rule },
    { label: 'Severity', value: <SeverityBadge severity={alert.severity} /> },
    { label: 'Raised On', value: formatDateTime(alert.raisedAt) },
    { label: 'Record', value: link === undefined ? record : <Link to={link}>{record}</Link> },
    { label: 'Status', value: <StatusBadge status={alert.status} /> },
    { label: 'What Happened', value: alert.message },
  ];
}
