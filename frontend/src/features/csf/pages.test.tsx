import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import type { ReactElement } from 'react';
import { Route, Routes } from 'react-router-dom';
import { csfApi } from '@/api/csf';
import { lovApi } from '@/api/lov';
import { reportApi } from '@/api/reports';
import { submittedWrapper } from '../submitted/testWrapper';
import ContactChangesPage from './changes/ContactChangesPage';
import {
  EPOLICY,
  PAYMENTS,
  account,
  change,
  document,
  page,
  searchResult,
  summary,
  verification,
} from './csfFixtures';
import CsfReportsPage from './reports/CsfReportsPage';
import CustomerSearchPage from './search/CustomerSearchPage';
import ServicingViewPage from './view/ServicingViewPage';

const AGENT = new Set([
  'CSF_VIEW',
  'CSF_CONTACT_UPDATE',
  'CSF_RESEND',
  'CSF_DOCUMENT_UPLOAD',
  'ATTACHMENT_VIEW',
]);
const SUPERVISOR = new Set([...AGENT, 'CSF_RESEND_OTHER', 'CSF_REPORT_VIEW']);
const MANAGEMENT = new Set(['CSF_VIEW', 'CSF_REPORT_VIEW']);

const OPTIONS: Record<string, { code: string; label: string }[]> = {
  CSF_CHANNEL: [{ code: 'HOTLINE', label: 'Hotline' }],
  CSF_VERIFY_CHECK: [
    { code: 'ADDRESS', label: 'Address' },
    { code: 'EMAIL', label: 'E-mail address' },
  ],
  CSF_CHANGE_REASON: [{ code: 'CLIENT_REQUEST', label: 'Client request' }],
  CSF_REFERRAL_FIELD: [{ code: 'CIVIL_STATUS', label: 'Civil status' }],
  CSF_DOCUMENT_TYPE: [{ code: 'CLIENT_REQUEST', label: 'Client request or letter' }],
  CSF_STATUS: [{ code: 'OPEN', label: 'Open' }],
};

function mockApis() {
  vi.spyOn(lovApi, 'options').mockImplementation((type: string) =>
    Promise.resolve(OPTIONS[type] ?? []),
  );
  vi.spyOn(csfApi, 'search').mockResolvedValue(searchResult());
  vi.spyOn(csfApi, 'summary').mockResolvedValue(summary());
  vi.spyOn(csfApi, 'accounts').mockResolvedValue([account()]);
  vi.spyOn(csfApi, 'payments').mockResolvedValue(PAYMENTS);
  vi.spyOn(csfApi, 'renewalAdvices').mockResolvedValue([document()]);
  vi.spyOn(csfApi, 'documents').mockResolvedValue([
    document(),
    document({ id: 22, fileName: 'id.heic', documentType: 'VALID_ID', recordType: 'Client' }),
  ]);
  vi.spyOn(csfApi, 'epolicies').mockResolvedValue([EPOLICY]);
  vi.spyOn(csfApi, 'history').mockResolvedValue([
    change(),
    change({ id: 42, changeNo: 'CSF-2026-000002', status: 'REFERRED', handoffStatus: 'OPEN' }),
  ]);
  vi.spyOn(csfApi, 'changes').mockResolvedValue(page([change()]));
  vi.spyOn(csfApi, 'preview').mockResolvedValue({
    documentName: 'ARN-2026-900001_RENEWAL_ADVICE.pdf',
    registeredEmail: 'maria.santos@seed-client.ph',
    subject: 'Your renewal advice',
    body: 'Dear client,\nplease find it attached.',
    otherAllowed: true,
  });
  vi.spyOn(reportApi, 'catalogue').mockResolvedValue([
    {
      code: 'CSF-ACTIVITY',
      title: 'Agent Activity',
      category: 'CUSTOMER_SERVICE',
      categoryLabel: 'Customer Service',
      description: 'Searches, views and changes per agent and day',
      parameters: [],
    },
  ]);
}

function show(ui: ReactElement, route = '/', perms: ReadonlySet<string> = AGENT) {
  render(<>{submittedWrapper(perms, 'csfagent', route)(ui)}</>);
}

function view(perms: ReadonlySet<string> = AGENT, route = '/csf/clients/1') {
  show(
    <Routes>
      <Route path="/csf/clients/:clientId" element={<ServicingViewPage />} />
      <Route path="/csf" element={<p>Search screen</p>} />
    </Routes>,
    route,
    perms,
  );
}

beforeEach(() => {
  mockApis();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('Customer Search', () => {
  it('refuses a short name and then lists the clients with their matching accounts', async () => {
    show(<CustomerSearchPage />, '/csf');
    fireEvent.change(screen.getByLabelText('Value'), { target: { value: 'Sa' } });
    fireEvent.click(screen.getByRole('button', { name: /Search/ }));
    expect(await screen.findByText('Enter at least 3 characters')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Value'), { target: { value: 'Santos' } });
    fireEvent.click(screen.getByRole('button', { name: /Search/ }));
    expect(await screen.findByText('Santos, Maria Clara Reyes')).toBeInTheDocument();
    expect(screen.getByText('ARN-2026-900001')).toBeInTheDocument();
    expect(csfApi.search).toHaveBeenCalledWith(1, 'NAME', 'Santos');
  });

  it('says no client was found with the criteria, and asks to refine a long list', async () => {
    vi.mocked(csfApi.search).mockResolvedValueOnce(
      searchResult({ keyType: 'PN_NO', value: 'PN-1', clients: [] }),
    );
    show(<CustomerSearchPage />, '/csf?key=PN_NO&q=PN-1');
    expect(await screen.findByText('No client found for PN No.: PN-1')).toBeInTheDocument();
  });

  it('warns when more clients match than shown', async () => {
    vi.mocked(csfApi.search).mockResolvedValueOnce(searchResult({ truncated: true }));
    show(<CustomerSearchPage />, '/csf?key=NAME&q=San');
    expect(await screen.findByText(/More than 50 clients match/)).toBeInTheDocument();
  });

  it('opens the Servicing View when one client matches', async () => {
    const one = searchResult();
    vi.mocked(csfApi.search).mockResolvedValue({ ...one, clients: one.clients.slice(0, 1) });
    show(
      <Routes>
        <Route path="/csf" element={<CustomerSearchPage />} />
        <Route path="/csf/clients/:clientId" element={<p>Servicing of client</p>} />
      </Routes>,
      '/csf',
    );
    fireEvent.change(screen.getByLabelText('Search By'), { target: { value: 'CLIENT_ID' } });
    fireEvent.change(screen.getByLabelText('Value'), { target: { value: 'CL-2026-000001' } });
    fireEvent.click(screen.getByRole('button', { name: /Search/ }));
    expect(await screen.findByText('Servicing of client')).toBeInTheDocument();
  });
});

describe('Servicing View', () => {
  it('shows the summary and every tab', async () => {
    view();
    expect(
      await screen.findByText('Santos, Maria Clara Reyes', { selector: 'h1' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Call after 3 pm', { exact: false })).toBeInTheDocument();
    expect(await screen.findByText('Motor Comprehensive')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: /Payments/ }));
    expect(await screen.findByText('OR-2026-000010')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Show Older' }));
    await waitFor(() => expect(csfApi.payments).toHaveBeenCalledWith(1, 1, 24, ''));
    fireEvent.click(screen.getByRole('tab', { name: /Renewal Advice/ }));
    expect(await screen.findByText('ARN-2026-900001_RENEWAL_ADVICE.pdf')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: /E-policies/ }));
    expect(await screen.findByText('ARN-2026-900001_EPOLICY_1.pdf')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: /Documents/ }));
    expect(await screen.findByText('id.heic')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: /Contact History/ }));
    expect(await screen.findByText('CSF-2026-000002')).toBeInTheDocument();
    expect(screen.getByText('With Fulfilment')).toBeInTheDocument();
  });

  it('shows a tab that cannot load without blanking the others', async () => {
    vi.mocked(csfApi.accounts).mockRejectedValue(new Error('down'));
    view();
    expect(await screen.findByText('Information not available now. Try again')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: /E-policies/ }));
    expect(await screen.findByText('ARN-2026-900001_EPOLICY_1.pdf')).toBeInTheDocument();
  });

  it('offers no contact action to management', async () => {
    view(MANAGEMENT);
    expect(
      await screen.findByText('Santos, Maria Clara Reyes', { selector: 'h1' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Update Contact/ })).not.toBeInTheDocument();
  });

  it('verifies the caller, refuses a failed verification and then changes the mobile', async () => {
    const verify = vi
      .spyOn(csfApi, 'verify')
      .mockResolvedValueOnce(verification('FAILED'))
      .mockResolvedValueOnce(verification('PASSED'));
    const save = vi.spyOn(csfApi, 'change').mockResolvedValue(change());
    view();
    fireEvent.click(await screen.findByRole('button', { name: /Update Contact/ }));
    const dialog = await screen.findByRole('dialog');
    await within(dialog).findByText('Address');
    fireEvent.change(within(dialog).getByLabelText(/Channel/), { target: { value: 'HOTLINE' } });
    const matched = within(dialog).getAllByLabelText('Matched');
    const notMatched = within(dialog).getAllByLabelText('Not Matched');
    fireEvent.click(matched[0]);
    fireEvent.click(notMatched[1]);
    fireEvent.click(within(dialog).getByRole('button', { name: 'Record Verification' }));
    expect(await within(dialog).findByText(/could not be verified/)).toBeInTheDocument();
    fireEvent.click(matched[1]);
    fireEvent.click(within(dialog).getByRole('button', { name: 'Record Verification' }));
    expect(await within(dialog).findByText('Caller verified')).toBeInTheDocument();
    expect(verify).toHaveBeenCalledTimes(2);
    fireEvent.change(within(dialog).getByLabelText(/Mobile/), { target: { value: '0917' } });
    expect(within(dialog).getByText('Enter a valid mobile number')).toBeInTheDocument();
    fireEvent.change(within(dialog).getByLabelText(/Mobile/), { target: { value: '09179998888' } });
    fireEvent.change(within(dialog).getByLabelText(/Reason/), {
      target: { value: 'CLIENT_REQUEST' },
    });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    await waitFor(() =>
      expect(save).toHaveBeenCalledWith(1, 1, {
        verificationId: 5,
        reasonCode: 'CLIENT_REQUEST',
        remarks: undefined,
        values: { MOBILE: '09179998888' },
      }),
    );
  });

  it('refers a civil status change to the fulfilment unit', async () => {
    const refer = vi
      .spyOn(csfApi, 'refer')
      .mockResolvedValue(change({ status: 'REFERRED', changeNo: 'CSF-2026-000003' }));
    view();
    fireEvent.click(await screen.findByRole('button', { name: /Refer to Fulfilment Unit/ }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.click(await within(dialog).findByLabelText('Civil status'));
    await within(dialog).findByText('Hotline');
    fireEvent.change(within(dialog).getByLabelText(/Channel/), { target: { value: 'HOTLINE' } });
    fireEvent.change(within(dialog).getByLabelText('Civil status asked for'), {
      target: { value: 'Married' },
    });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Refer' }));
    await waitFor(() =>
      expect(refer).toHaveBeenCalledWith(1, 1, {
        channel: 'HOTLINE',
        fields: { CIVIL_STATUS: 'Married' },
        remarks: undefined,
      }),
    );
  });

  it('lets a supervisor resend a renewal advice to another address with a reason', async () => {
    const resend = vi.spyOn(csfApi, 'resend').mockResolvedValue({
      recipient: 'other@example.ph',
      messageId: 9,
      documentName: 'RA.pdf',
    });
    view(SUPERVISOR, '/csf/clients/1?tab=advices');
    fireEvent.click(await screen.findByRole('button', { name: /Resend/ }));
    const dialog = await screen.findByRole('dialog');
    expect(await within(dialog).findByText('Your renewal advice')).toBeInTheDocument();
    fireEvent.click(within(dialog).getByLabelText('Send to another address'));
    fireEvent.change(within(dialog).getByLabelText(/Recipient/), {
      target: { value: 'other@example.ph' },
    });
    expect(within(dialog).getByRole('button', { name: 'Resend' })).toBeDisabled();
    fireEvent.change(within(dialog).getByLabelText(/Reason/), { target: { value: 'Abroad' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Resend' }));
    await waitFor(() =>
      expect(resend).toHaveBeenCalledWith(1, 1, 'RA', {
        documentId: 21,
        recipient: 'other@example.ph',
        reason: 'Abroad',
      }),
    );
  });

  it('uploads a document to an account', async () => {
    const upload = vi.spyOn(csfApi, 'upload').mockResolvedValue(document());
    view(AGENT, '/csf/clients/1?tab=documents');
    fireEvent.click(await screen.findByRole('button', { name: /Upload Document/ }));
    const dialog = await screen.findByRole('dialog');
    await within(dialog).findByText('Account ARN-2026-900001');
    fireEvent.change(within(dialog).getByLabelText(/Attach To/), { target: { value: '11' } });
    fireEvent.change(within(dialog).getByLabelText(/Document Type/), {
      target: { value: 'CLIENT_REQUEST' },
    });
    const file = new File(['%PDF-1.4'], 'letter.pdf', { type: 'application/pdf' });
    const input = dialog.querySelector('input[type="file"]');
    expect(input).not.toBeNull();
    if (input) {
      fireEvent.change(input, { target: { files: [file] } });
    }
    fireEvent.click(within(dialog).getByRole('button', { name: 'Upload' }));
    await waitFor(() =>
      expect(upload).toHaveBeenCalledWith(1, 1, file, {
        accountId: 11,
        documentType: 'CLIENT_REQUEST',
        description: undefined,
      }),
    );
  });
});

describe('Contact Changes and reports', () => {
  it('lists the changes with their fields', async () => {
    show(<ContactChangesPage />, '/csf/changes', SUPERVISOR);
    expect(await screen.findByText('CSF-2026-000001')).toBeInTheDocument();
    expect(screen.getByText('09175550199')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Status'), { target: { value: 'REFUSED' } });
    await waitFor(() =>
      expect(csfApi.changes).toHaveBeenLastCalledWith(1, { status: 'REFUSED' }, 0, 25),
    );
  });

  it('lists the Customer Service reports', async () => {
    show(<CsfReportsPage />, '/csf/reports', MANAGEMENT);
    expect(await screen.findByText('Agent Activity')).toBeInTheDocument();
  });
});
