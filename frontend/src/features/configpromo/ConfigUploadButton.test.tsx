import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';
import type { ConfigUploadType } from '@/api/configUploads';
import { ConfigUploadButton } from './ConfigUploadButton';

const type = (code: string, templateId: string, mayUpload = true): ConfigUploadType => ({
  code,
  templateId,
  title: templateId,
  screen: 'Setup',
  filledBy: 'Comptrollership',
  rules: [],
  mayUpload,
  mayApprove: true,
});

function wrap(types: ConfigUploadType[], children: ReactNode) {
  const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  queries.setQueryData(['config-uploads', 'types'], types);
  return (
    <QueryClientProvider client={queries}>
      <MemoryRouter>{children}</MemoryRouter>
    </QueryClientProvider>
  );
}

describe('upload action of the master data screens', () => {
  it('offers the currency and exchange rate templates on Currencies & Exchange Rates', () => {
    render(
      wrap(
        [type('CFG_CURRENCY', 'MD-02'), type('CFG_EXCHANGE_RATE', 'MD-03')],
        <ConfigUploadButton types={['CFG_CURRENCY', 'CFG_EXCHANGE_RATE']} />,
      ),
    );
    expect(screen.getByRole('button', { name: 'Upload' })).toBeInTheDocument();
  });

  it('offers the party tax profile and tax form uploads to the tax maintainers only', () => {
    const { unmount } = render(
      wrap(
        [type('CFG_PARTY_TAX_PROFILE', 'TX-01')],
        <ConfigUploadButton types={['CFG_PARTY_TAX_PROFILE']} />,
      ),
    );
    expect(screen.getByRole('button', { name: 'Upload' })).toBeInTheDocument();
    unmount();
    render(
      wrap([type('CFG_TAX_FORM', 'TX-02', false)], <ConfigUploadButton types={['CFG_TAX_FORM']} />),
    );
    expect(screen.queryByRole('button', { name: 'Upload' })).toBeNull();
  });
});
