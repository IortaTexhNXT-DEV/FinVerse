import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { attachmentsApi } from '@/api/attachments';
import type { AttachmentInfo } from '@/api/attachments';
import { lovApi } from '@/api/lov';
import { AuthContext } from '@/auth/authContext';
import { ToastContext } from '@/components/ui/toastContext';
import { Attachments } from './Attachments';

const file = (id: number, name: string, linked = false): AttachmentInfo => ({
  id,
  entityType: 'Account',
  entityId: '7',
  fileName: name,
  contentType: 'application/pdf',
  sizeBytes: 1000,
  sha256: 'abcdef0123456789',
  uploadedBy: 'ao',
  uploadedAt: '2026-09-01T00:00:00Z',
  documentType: 'IDF',
  linked,
});

function wrap(children: ReactNode) {
  const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const auth = {
    user: null,
    loading: false,
    login: () => Promise.resolve(),
    logout: () => undefined,
    can: () => true,
    passwordChange: null,
    passwordChanged: () => undefined,
  };
  const toast = { success: vi.fn(), error: vi.fn() };
  return (
    <QueryClientProvider client={queries}>
      <AuthContext.Provider value={auth}>
        <ToastContext.Provider value={toast}>{children}</ToastContext.Provider>
      </AuthContext.Provider>
    </QueryClientProvider>
  );
}

describe('attachments', () => {
  beforeEach(() => {
    vi.spyOn(attachmentsApi, 'list').mockResolvedValue([file(1, 'a.pdf'), file(2, 'b.pdf', true)]);
    vi.spyOn(attachmentsApi, 'policy').mockResolvedValue({
      maxSizeBytes: 10_000,
      allowedExtensions: 'pdf, msg',
      namingSyntax: '<REFERENCE>_<DOCTYPE>_<n>.<ext>',
      maxFiles: 3,
    });
    vi.spyOn(lovApi, 'options').mockResolvedValue([{ code: 'IDF', label: 'IDF' }]);
  });
  afterEach(() => vi.restoreAllMocks());

  it('downloads the selected files as one ZIP named after the reference', async () => {
    const zip = vi
      .spyOn(attachmentsApi, 'zip')
      .mockResolvedValue({ blob: new Blob(['z']), fileName: 'ARN-1.zip' });
    const save = vi.fn(() => 'blob:zip');
    URL.createObjectURL = save;
    URL.revokeObjectURL = vi.fn();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    render(wrap(<Attachments entityType="Account" entityId={7} reference="ARN-1" />));
    await screen.findByText('a.pdf');
    expect(screen.getByLabelText('Unlink b.pdf')).toBeInTheDocument();
    const button = screen.getByRole('button', { name: /Download ZIP/ });
    expect(button).toBeDisabled();
    fireEvent.click(screen.getByLabelText('Select a.pdf'));
    fireEvent.click(screen.getByLabelText('Select b.pdf'));
    fireEvent.click(button);
    await waitFor(() => expect(zip).toHaveBeenCalledWith([1, 2], 'ARN-1'));
    await waitFor(() => expect(save).toHaveBeenCalled());
  });

  it('uploads several files with the chosen document type and refuses too many', async () => {
    const upload = vi.spyOn(attachmentsApi, 'uploadMany').mockResolvedValue([file(3, 'c.pdf')]);
    render(wrap(<Attachments entityType="Account" entityId={7} reference="ARN-1" />));
    await screen.findByText('a.pdf');
    await screen.findByRole('option', { name: 'IDF' });
    fireEvent.change(screen.getByLabelText('Document type'), { target: { value: 'IDF' } });
    fireEvent.click(screen.getByRole('checkbox', { name: /Name as ARN-1_IDF_n/ }));
    const input = screen.getByLabelText('Files to attach');
    const pdf = (name: string) => new File(['x'], name, { type: 'application/pdf' });
    fireEvent.change(input, {
      target: { files: [pdf('1.pdf'), pdf('2.pdf'), pdf('3.pdf'), pdf('4.pdf')] },
    });
    expect(await screen.findByRole('alert')).toHaveTextContent('Choose at most 3 files');
    fireEvent.change(input, { target: { files: [pdf('1.pdf'), pdf('2.pdf')] } });
    await waitFor(() => expect(upload).toHaveBeenCalledTimes(1));
    const [, , files, options] = upload.mock.calls[0] ?? [];
    expect(files).toHaveLength(2);
    expect(options).toMatchObject({ documentType: 'IDF', naming: 'NOMINATE', reference: 'ARN-1' });
  });
});
