import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { ProductLineLabel, ProductName } from './LovLabel';

function wrap(children: ReactNode) {
  const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  queries.setQueryData(
    ['catalog', 'products', 'names'],
    [{ code: 'MTR15', name: 'Motor Comprehensive – Private Car', lineCode: 'MOTOR' }],
  );
  queries.setQueryData(['catalog', 'lines'], [{ code: 'MOTOR', name: 'Motor' }]);
  return <QueryClientProvider client={queries}>{children}</QueryClientProvider>;
}

describe('product names', () => {
  it('shows the product name, with the code as the muted second line of a list cell', () => {
    render(wrap(<ProductName code="MTR15" withCode />));
    expect(screen.getByText('Motor Comprehensive – Private Car')).toBeInTheDocument();
    expect(screen.getByText('MTR15')).toHaveClass('muted');
  });

  it('does not repeat a code the product name already carries', () => {
    const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queries.setQueryData(
      ['catalog', 'products', 'names'],
      [{ code: 'MTR10', name: 'Motor Comprehensive Package MTR10', lineCode: 'MOTOR' }],
    );
    render(
      <QueryClientProvider client={queries}>
        <ProductName code="MTR10" withCode />
      </QueryClientProvider>,
    );
    expect(screen.getByText('Motor Comprehensive Package MTR10')).toBeInTheDocument();
    expect(screen.queryByText('MTR10')).toBeNull();
  });

  it('keeps the code in the tooltip where only the name is shown', () => {
    render(wrap(<ProductName code="MTR15" />));
    expect(screen.getByText('Motor Comprehensive – Private Car')).toHaveAttribute('title', 'MTR15');
  });

  it('names the product and its line on record pages', () => {
    render(wrap(<ProductLineLabel product="MTR15" line="MOTOR" />));
    expect(screen.getByText('Motor Comprehensive – Private Car (Motor)')).toBeInTheDocument();
  });

  it('shows a dash when there is no product', () => {
    render(wrap(<ProductName code={undefined} />));
    expect(screen.getByText('—')).toHaveClass('muted');
  });
});
