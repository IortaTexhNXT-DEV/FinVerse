import { checkFile, extensionOf, formatBytes } from './files';

describe('file helpers', () => {
  it('formats sizes', () => {
    expect(formatBytes(512)).toBe('512 B');
    expect(formatBytes(1536)).toBe('1.5 KB');
    expect(formatBytes(10 * 1024 * 1024)).toBe('10.0 MB');
    expect(formatBytes(5 * 1024 ** 4)).toBe('5120.0 GB');
  });

  it('extracts extensions', () => {
    expect(extensionOf('Report.PDF')).toBe('pdf');
    expect(extensionOf('archive.tar.gz')).toBe('gz');
    expect(extensionOf('README')).toBe('');
  });

  it('pre-checks uploads', () => {
    const allowed = ['pdf', 'csv'];
    expect(checkFile({ name: 'a.pdf', size: 10 }, allowed, 100)).toBeUndefined();
    expect(checkFile({ name: 'a.pdf', size: 0 }, allowed, 100)).toBe('The file is empty.');
    expect(checkFile({ name: 'a.pdf', size: 200 }, allowed, 100)).toContain('larger');
    expect(checkFile({ name: 'a.exe', size: 10 }, allowed, 100)).toContain('pdf, csv');
  });
});
