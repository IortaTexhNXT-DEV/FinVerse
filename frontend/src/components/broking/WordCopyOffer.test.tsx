import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { isPdf, PDF_SAVED_EVENT, saveFile } from '@/api/client';
import { docRenditionsApi, sha256Hex } from '@/api/docRenditions';
import { ToastContext } from '@/components/ui/toastContext';
import { WordCopyOffer } from './WordCopyOffer';

const toast = { success: vi.fn(), error: vi.fn() };

function wrap(children: ReactNode) {
  const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={queries}>
      <ToastContext.Provider value={toast}>{children}</ToastContext.Provider>
    </QueryClientProvider>
  );
}

const pdf = () => new Blob(['%PDF-1.7 slip'], { type: 'application/pdf' });

describe('Word copy of a downloaded document (client requirement 16)', () => {
  let saved: string[];

  beforeEach(() => {
    saved = [];
    URL.createObjectURL = vi.fn(() => 'blob:x');
    URL.revokeObjectURL = vi.fn();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      saved.push(this.download);
    });
  });
  afterEach(() => vi.restoreAllMocks());

  it('recognises PDFs by type or name', () => {
    expect(isPdf(new Blob([''], { type: 'application/pdf' }), 'x')).toBe(true);
    expect(isPdf(new Blob(['']), 'Slip.PDF')).toBe(true);
    expect(isPdf(new Blob(['']), 'Slip.docx')).toBe(false);
  });

  it('hashes files as SHA-256 hex', async () => {
    await expect(sha256Hex(new Blob(['abc']))).resolves.toBe(
      'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad',
    );
  });

  it('offers the Word copy after a composed PDF is saved and downloads it', async () => {
    const availability = vi
      .spyOn(docRenditionsApi, 'availability')
      .mockResolvedValue({ available: true, title: 'Placement Slip' });
    const word = vi
      .spyOn(docRenditionsApi, 'word')
      .mockResolvedValue({ blob: new Blob(['docx']), fileName: 'PL-1.docx' });
    render(wrap(<WordCopyOffer />));
    act(() => saveFile(pdf(), 'PL-2026-0001.pdf'));
    expect(await screen.findByText('Placement Slip')).toBeInTheDocument();
    expect(screen.getByText('PL-2026-0001.pdf is also available in Word.')).toBeInTheDocument();
    expect(availability).toHaveBeenCalledWith(expect.stringMatching(/^[0-9a-f]{64}$/));
    fireEvent.click(screen.getByRole('button', { name: 'Download Word' }));
    await waitFor(() => expect(word).toHaveBeenCalledWith(expect.any(Blob), 'PL-2026-0001.pdf'));
    await waitFor(() => expect(saved).toEqual(['PL-2026-0001.pdf', 'PL-2026-0001.docx']));
    expect(toast.success).toHaveBeenCalledWith('PL-2026-0001.docx downloaded');
    expect(screen.queryByText('Placement Slip')).not.toBeInTheDocument();
  });

  it('offers nothing for PDFs without a Word copy and can be dismissed', async () => {
    const availability = vi
      .spyOn(docRenditionsApi, 'availability')
      .mockResolvedValueOnce({ available: false })
      .mockResolvedValueOnce({ available: true });
    render(wrap(<WordCopyOffer />));
    act(() => {
      globalThis.dispatchEvent(
        new CustomEvent(PDF_SAVED_EVENT, { detail: { blob: pdf(), fileName: 'report.pdf' } }),
      );
    });
    await waitFor(() => expect(availability).toHaveBeenCalledTimes(1));
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    act(() => saveFile(pdf(), 'advice.pdf'));
    expect(await screen.findByText('advice.pdf')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }));
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('reports a failed Word download', async () => {
    vi.spyOn(docRenditionsApi, 'availability').mockResolvedValue({ available: true, title: 'SOA' });
    vi.spyOn(docRenditionsApi, 'word').mockRejectedValue(new Error('Not found'));
    render(wrap(<WordCopyOffer />));
    act(() => saveFile(pdf(), 'soa.pdf'));
    fireEvent.click(await screen.findByRole('button', { name: 'Download Word' }));
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Not found'));
  });
});
