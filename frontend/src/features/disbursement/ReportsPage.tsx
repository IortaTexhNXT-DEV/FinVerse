import { FileBarChart } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import './disbursement.css';

interface ReportLink {
  code: string;
  name: string;
  text: string;
}

/** Disbursement reports (DIS 3.28.x) grouped as on the BRD; they run in the report runner. */
const REPORT_GROUPS: readonly { title: string; reports: readonly ReportLink[] }[] = [
  {
    title: 'Payments',
    reports: [
      {
        code: 'DSB-MASTERLIST',
        name: 'Masterlist of Disbursements',
        text: 'Vouchers of a period with payee, mode, instrument and status.',
      },
      {
        code: 'DSB-UNRELEASED-CHECKS',
        name: 'Unreleased Checks',
        text: 'Printed checks not yet released to the payee.',
      },
      {
        code: 'DSB-ML-STALE',
        name: 'Miscellaneous Liability - Stale Checks',
        text: 'Checks past the stale period and their re-issue.',
      },
      {
        code: 'DSB-ATD',
        name: 'Authority to Debit',
        text: 'ATDs e-mailed to the branches and their debit confirmation.',
      },
      {
        code: 'DSB-CASH-FLOW',
        name: 'Disbursement Cash Flow',
        text: 'Payments by paying account and value date.',
      },
      {
        code: 'DSB-CWT-COMMISSION',
        name: 'CWT / BIR 2307 on Commission',
        text: 'CWT certificates received from insurers and released to suppliers.',
      },
    ],
  },
  {
    title: 'Control',
    reports: [
      {
        code: 'DSB-PAYEE',
        name: 'Payee Report',
        text: 'Payees with their class, modes and status.',
      },
      {
        code: 'DSB-PAYEE-NOMATCH',
        name: 'Payees Not Matched',
        text: 'Payment requests waiting for their payee.',
      },
      {
        code: 'DSB-UPLOAD-FALLOUT',
        name: 'Request Upload Fall-out',
        text: 'Rows of the disbursement uploads that failed.',
      },
      {
        code: 'DSB-UNREGULARIZED',
        name: 'Unregularised Transactions',
        text: 'Approved vouchers without OR / AR received.',
      },
    ],
  },
  {
    title: 'End of Day',
    reports: [
      {
        code: 'DSB-EOD-REMIT',
        name: 'Remittance End-of-Day',
        text: 'Remittances to insurers paid on the business date.',
      },
      {
        code: 'DSB-EOD-REFUND',
        name: 'Refund End-of-Day',
        text: 'Client refunds paid on the business date.',
      },
      {
        code: 'DSB-EOD-SUPPLIER',
        name: 'Payment to Supplier End-of-Day',
        text: 'Supplier and agency payments of the business date.',
      },
      {
        code: 'DSB-EOD-EMPLOYEE',
        name: 'Employee-related End-of-Day',
        text: 'Employee reimbursements of the business date.',
      },
      {
        code: 'DSB-EOD-OTHER',
        name: 'Other Disbursements End-of-Day',
        text: 'Other disbursement types of the business date.',
      },
      {
        code: 'DSB-EOD-SUMMARY',
        name: 'Disbursement Summary End-of-Day',
        text: 'Counts and amounts by mode and type of the business date.',
      },
    ],
  },
];

/**
 * Disbursement reports (DIS 3.28.x): the payment, control and end-of-day reports, each opened in
 * the report runner with its parameters and PDF, Excel and CSV exports.
 */
export default function ReportsPage() {
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Disbursement"
        title="Disbursement Reports"
        description="Payment, control and end-of-day reports; each opens in the report runner with its exports."
      />
      {REPORT_GROUPS.map((group) => (
        <Card key={group.title} title={group.title}>
          <ul className="dsb-reports">
            {group.reports.map((r) => (
              <li key={r.code}>
                <FileBarChart size={16} aria-hidden="true" />
                <div>
                  <Link to={`/reports/${r.code}`}>{r.name}</Link>
                  <div className="dsb-muted">{r.text}</div>
                </div>
              </li>
            ))}
          </ul>
        </Card>
      ))}
    </div>
  );
}
