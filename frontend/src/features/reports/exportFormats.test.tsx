import { fireEvent, render, screen } from '@testing-library/react';
import { ExportButtons } from './ExportButtons';
import { DOCUMENT_FORMATS, formatLabel, menuFormats, wordFileName } from './exportFormats';

describe('export formats (client requirement 16)', () => {
  it('offers Excel and PDF on every report and Word only on documents and schedules', () => {
    expect(menuFormats(undefined)).toEqual(['XLSX', 'PDF', 'ODS', 'CSV', 'XML']);
    expect(menuFormats({ documentStyle: false, formats: ['PDF', 'CSV'] })).toEqual([
      'XLSX',
      'PDF',
      'CSV',
    ]);
    expect(
      menuFormats({ documentStyle: true, formats: ['PDF', 'XLSX', 'CSV', 'ODS', 'XML', 'DOCX'] }),
    ).toEqual(['XLSX', 'PDF', 'DOCX', 'ODS', 'CSV', 'XML']);
    expect(menuFormats({ documentStyle: false, formats: ['PDF', 'DOCX'] })).not.toContain('DOCX');
    expect(menuFormats({ documentStyle: true }, ['XLSX', 'PDF', 'DOCX'])).toEqual([
      'XLSX',
      'PDF',
      'DOCX',
    ]);
    expect(DOCUMENT_FORMATS).toEqual(['PDF', 'DOCX']);
  });

  it('labels formats and names the Word copy of a PDF', () => {
    expect(formatLabel('XLSX')).toBe('Excel');
    expect(formatLabel('DOCX')).toBe('Word');
    expect(formatLabel('ZIP')).toBe('ZIP');
    expect(wordFileName('QS-2026-0001.pdf')).toBe('QS-2026-0001.docx');
    expect(wordFileName('Slip.PDF')).toBe('Slip.docx');
    expect(wordFileName('.pdf')).toBe('document.docx');
  });
});

describe('export buttons', () => {
  it('shows one button per format and exports the chosen one', () => {
    const onExport = vi.fn();
    render(<ExportButtons formats={['XLSX', 'PDF', 'DOCX']} onExport={onExport} />);
    expect(screen.getAllByRole('button').map((b) => b.textContent)).toEqual([
      'Excel',
      'PDF',
      'Word',
    ]);
    fireEvent.click(screen.getByRole('button', { name: 'Word' }));
    expect(onExport).toHaveBeenCalledWith('DOCX');
  });

  it('prefixes labels, shows the pending format busy and can be disabled', () => {
    const onExport = vi.fn();
    const { rerender } = render(
      <ExportButtons
        formats={['PDF', 'CSV']}
        prefix="Export to"
        pending="PDF"
        onExport={onExport}
      />,
    );
    expect(screen.getByRole('button', { name: 'Export to PDF' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Export to CSV' })).toBeEnabled();
    rerender(<ExportButtons formats={['CSV']} disabled onExport={onExport} />);
    expect(screen.getByRole('button', { name: 'CSV' })).toBeDisabled();
  });
});
