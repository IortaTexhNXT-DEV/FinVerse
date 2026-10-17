import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { misFieldsApi } from '@/api/misFields';
import type { ReportResult } from '@/api/reports';
import { ebWrapper } from '@/features/eb/testWrapper';
import { LayoutPanel } from './LayoutPanel';
import { NO_LAYOUT } from './reportLayout';

const RESULT: ReportResult = {
  code: 'GL-COA',
  title: 'Chart of Accounts',
  parameterEcho: [],
  columns: [
    { key: 'status', label: 'Status', type: 'TEXT', summed: false },
    { key: 'name', label: 'Name', type: 'TEXT', summed: false },
  ],
  rows: [],
  notes: [],
};

describe('Layout panel of the report runner', () => {
  afterEach(() => vi.restoreAllMocks());

  it('groups by an MIS field and asks for a grouping field before a chart', async () => {
    vi.spyOn(misFieldsApi, 'list').mockResolvedValue([
      {
        id: 1,
        code: 'STATUS',
        label: 'Status',
        entity: 'ACCOUNT',
        source: 'STATUS',
        columnKeys: ['status', 'stage'],
        lovType: null,
        recordStatus: 'ACTIVE',
        maker: 'SYSTEM',
        authorizedBy: 'SYSTEM',
        authorizedAt: null,
      },
    ]);
    const onApply = vi.fn();
    render(
      ebWrapper(new Set(['REPORT_VIEW']))(
        <LayoutPanel result={RESULT} layout={NO_LAYOUT} busy={false} onApply={onApply} />,
      ),
    );
    expect(
      (await screen.findAllByRole('option', { name: 'Status (MIS field)' })).length,
    ).toBeGreaterThan(0);
    await userEvent.selectOptions(screen.getByLabelText('Chart'), 'BAR');
    expect(screen.getByText('Select a grouping field for the chart')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Apply Layout/ })).toBeDisabled();
    await userEvent.selectOptions(screen.getByLabelText('Group by'), 'status');
    await userEvent.click(screen.getByRole('button', { name: /Apply Layout/ }));
    expect(onApply).toHaveBeenCalledWith({
      group1: 'status',
      group2: '',
      chart: 'BAR',
      chartValue: 'COUNT',
    });
  });
});
