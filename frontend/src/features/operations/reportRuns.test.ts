import { fileAvailable, runStatus } from './reportRuns';

describe('archived report runs', () => {
  it('labels views, exports and generated files', () => {
    expect(runStatus({ action: 'VIEW' })).toBe('VIEWED');
    expect(runStatus({ action: 'EXPORT', format: 'PDF' })).toBe('EXPORTED_PDF');
    expect(runStatus({ action: 'GENERATE', format: 'CSV' })).toBe('GENERATED_CSV');
  });

  it('offers a generated file from its availability time only', () => {
    const now = new Date('2026-09-25T08:00:00Z');
    expect(fileAvailable({ action: 'VIEW' }, now)).toBe(false);
    expect(fileAvailable({ action: 'EXPORT' }, now)).toBe(true);
    expect(fileAvailable({ action: 'GENERATE' }, now)).toBe(true);
    expect(fileAvailable({ action: 'GENERATE', availableFrom: '2026-09-25T07:59:00Z' }, now)).toBe(
      true,
    );
    expect(fileAvailable({ action: 'GENERATE', availableFrom: '2026-09-28T00:00:00Z' }, now)).toBe(
      false,
    );
  });
});
