import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { OpsInvoiceSummary } from '@/api/operations';
import { AUDIT_COLUMN, INVOICE_COLUMNS, invoiceAuditLink } from './invoiceColumns';

describe('invoice search columns', () => {
  it('pairs the amounts and the statuses as the FRS columns, so the list fits its card', () => {
    expect(INVOICE_COLUMNS.map((c) => c.header)).toEqual([
      'Invoice No.',
      'Assured / Client Code',
      'Insurer',
      'Booked / Inception',
      'Account Officer',
      'Gross Premium / Outstanding',
      'Status / Remittance',
      'Flags',
      'Cover / Version',
      'Marketing / Processing',
      'Placement Status',
      'Approved by Processing',
      'Cancellation',
    ]);
    // The Invoice Master List values are chosen with Columns.
    expect(INVOICE_COLUMNS.slice(8).every((c) => c.defaultHidden === true)).toBe(true);
    expect(AUDIT_COLUMN.defaultHidden).toBe(true);
  });

  it('shows the account values, the cancellation and the audit trail link', () => {
    const invoice = {
      invoiceNo: 'BI-2026-000001',
      bookingDate: '2026-03-01',
      flags: { cancelled: true },
      master: {
        coverNo: 'CV-77',
        versionNo: 2,
        cancelledOn: '2026-04-01',
        cancellationReason: 'Sold the vehicle',
      },
    } as unknown as OpsInvoiceSummary;
    const cell = (key: string) =>
      INVOICE_COLUMNS.find((c) => c.key === key)?.render(invoice) ?? null;
    render(
      <MemoryRouter>
        <div>{cell('cover')}</div>
        <div>{cell('cancellation')}</div>
        <div>{cell('approved')}</div>
        <div>{AUDIT_COLUMN.render(invoice)}</div>
      </MemoryRouter>,
    );
    expect(screen.getByText('CV-77')).toBeVisible();
    expect(screen.getByText('Version 2')).toBeVisible();
    expect(screen.getByText('Sold the vehicle')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Audit Trail' })).toHaveAttribute(
      'href',
      invoiceAuditLink(invoice),
    );
    expect(invoiceAuditLink(invoice)).toBe(
      '/admin/audit?entityType=OpsInvoice&entityId=BI-2026-000001&from=2026-03-01',
    );
  });

  it('reads In force for an invoice without cancellation', () => {
    const invoice = { flags: { cancelled: false }, master: null } as unknown as OpsInvoiceSummary;
    const cancellation = INVOICE_COLUMNS.find((c) => c.key === 'cancellation');
    render(<div>{cancellation?.render(invoice)}</div>);
    expect(screen.getByText('In force')).toBeVisible();
  });
});
