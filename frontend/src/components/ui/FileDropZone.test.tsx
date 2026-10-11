import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { acceptedTypesText } from '@/utils/presentation';
import { FileDropZone } from './FileDropZone';

function pdf(name = 'policy.pdf', size = 1024): File {
  return new File([new Uint8Array(size)], name, { type: 'application/pdf' });
}

describe('FileDropZone', () => {
  it('shows the accepted types and the maximum size as one hint', () => {
    render(<FileDropZone label="File" accept=".xlsx,.ods,.csv" onChange={vi.fn()} />);
    expect(screen.getByText('XLSX, ODS or CSV · max 20 MB')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Browse' })).toBeInTheDocument();
  });

  it('lists the chosen file with its size and removes it', async () => {
    const onChange = vi.fn();
    render(<FileDropZone label="File" accept=".pdf" onChange={onChange} />);
    await userEvent.upload(screen.getByLabelText('File', { selector: 'input' }), pdf());
    expect(onChange).toHaveBeenLastCalledWith([expect.any(File)]);
    expect(screen.getByText('policy.pdf')).toBeInTheDocument();
    expect(screen.getByText('1.0 KB')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Remove policy.pdf' }));
    expect(onChange).toHaveBeenLastCalledWith([]);
    expect(screen.queryByText('policy.pdf')).not.toBeInTheDocument();
  });

  it('accepts dropped files and refuses other types and files that are too large', () => {
    const onChange = vi.fn();
    render(<FileDropZone label="File" accept=".pdf" maxSizeMb={1} multiple onChange={onChange} />);
    const zone = screen.getByRole('region', { name: 'File' });
    fireEvent.drop(zone, { dataTransfer: { files: [pdf('a.pdf'), pdf('b.pdf')] } });
    expect(onChange).toHaveBeenLastCalledWith([expect.any(File), expect.any(File)]);
    fireEvent.drop(zone, {
      dataTransfer: { files: [new File(['x'], 'notes.txt', { type: 'text/plain' })] },
    });
    expect(screen.getByRole('alert')).toHaveTextContent('notes.txt is not an accepted file type.');
    fireEvent.drop(zone, { dataTransfer: { files: [pdf('big.pdf', 2 * 1024 * 1024)] } });
    expect(screen.getByRole('alert')).toHaveTextContent('big.pdf is larger than 1 MB.');
  });

  it('shows the progress state while uploading', () => {
    render(<FileDropZone label="File" busy progress={40} onChange={vi.fn()} />);
    expect(screen.getByRole('progressbar', { name: 'Uploading' })).toHaveAttribute(
      'aria-valuenow',
      '40',
    );
  });

  it('describes accepted types', () => {
    expect(acceptedTypesText('.pdf')).toBe('PDF');
    expect(acceptedTypesText('.csv,text/csv')).toBe('CSV');
    expect(acceptedTypesText(undefined)).toBe('');
  });
});
