import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { InsurerResponse } from '@/api/productmaint';
import { DataTable } from '@/components/ui/DataTable';
import { responseColumns } from './responseColumns';

const RESPONSE = {
  id: 1,
  insurerCode: 'INS-MGIC',
  insurerName: 'Mabuhay General Insurance Corp.',
  outcome: 'PENDING',
  revision: 0,
} as unknown as InsurerResponse;

describe('insurer response columns', () => {
  it('offers Key In and Resend in the row action menu, never as buttons in the row', () => {
    render(
      <DataTable
        columns={responseColumns(
          true,
          () => undefined,
          () => undefined,
        )}
        rows={[RESPONSE]}
        rowKey={(r) => r.id}
      />,
    );
    expect(
      screen.getByRole('button', { name: 'Actions for Mabuhay General Insurance Corp.' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /key in/i })).toBeNull();
  });
});
