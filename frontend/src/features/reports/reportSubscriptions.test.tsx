import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { recipientsOf, reportSubscriptionApi, scheduleText } from '@/api/reportSubscriptions';
import type { ReportSubscription } from '@/api/reportSubscriptions';
import { ebWrapper } from '@/features/eb/testWrapper';
import ReportSubscriptionsPage from './ReportSubscriptionsPage';
import { SubscribeDialog } from './SubscribeDialog';

const SUB: ReportSubscription = {
  id: 4,
  reportCode: 'GL-COA',
  username: 'comptrol',
  parameters: { companyId: '1' },
  frequency: 'WEEKLY',
  dayOfWeek: 1,
  dayOfMonth: null,
  format: 'CSV',
  recipients: ['finance@subscriber.example'],
  active: true,
  lastRunDate: null,
  createdAt: '2026-10-01T00:00:00Z',
};

describe('report subscriptions', () => {
  it('describes schedules and reads recipients', () => {
    expect(scheduleText(SUB)).toBe('Weekly on Monday');
    expect(scheduleText({ frequency: 'MONTHLY', dayOfWeek: null, dayOfMonth: 31 })).toBe(
      'Monthly on day 31',
    );
    expect(scheduleText({ frequency: 'DAILY', dayOfWeek: null, dayOfMonth: null })).toBe('Daily');
    expect(recipientsOf('a@x.example, b@x.example;c@x.example')).toHaveLength(3);
  });

  it('lists the subscriptions, their runs and stops one', async () => {
    vi.spyOn(reportSubscriptionApi, 'mine').mockResolvedValue([SUB]);
    const runs = vi.spyOn(reportSubscriptionApi, 'runs').mockResolvedValue([
      {
        id: 1,
        runDate: '2026-10-12',
        status: 'DENIED',
        reportRunId: null,
        rowCount: 0,
        detail: 'Company 2 is outside the data scope',
        runAt: '2026-10-12T22:00:00Z',
      },
    ]);
    const stop = vi
      .spyOn(reportSubscriptionApi, 'deactivate')
      .mockResolvedValue({ ...SUB, active: false });
    render(ebWrapper(new Set(['REPORT_VIEW']), 'comptrol')(<ReportSubscriptionsPage />));
    expect(await screen.findByText('Weekly on Monday')).toBeInTheDocument();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Runs' }));
    expect(await screen.findByText('Company 2 is outside the data scope')).toBeInTheDocument();
    expect(runs).toHaveBeenCalledWith(4);
    await user.click(screen.getByRole('button', { name: 'Stop' }));
    expect(stop).toHaveBeenCalledWith(4);
  });

  it('subscribes with the parameters of the screen', async () => {
    const subscribe = vi.spyOn(reportSubscriptionApi, 'subscribe').mockResolvedValue(SUB);
    render(
      ebWrapper(new Set(['REPORT_VIEW']))(
        <SubscribeDialog
          reportCode="GL-COA"
          title="Chart of Accounts"
          parameters={{ companyId: '1' }}
          formats={['CSV', 'PDF']}
          onClose={() => undefined}
        />,
      ),
    );
    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/Recipients/), 'finance@subscriber.example');
    await user.click(screen.getByRole('button', { name: 'Subscribe' }));
    expect(subscribe).toHaveBeenCalledWith(
      expect.objectContaining({
        reportCode: 'GL-COA',
        parameters: { companyId: '1' },
        frequency: 'DAILY',
        format: 'CSV',
        recipients: ['finance@subscriber.example'],
      }),
    );
  });
});
