/** File helpers shared by attachments and uploads. */

const UNITS = ['B', 'KB', 'MB', 'GB'];

/** Human readable size, e.g. 1536 -> "1.5 KB". */
export function formatBytes(bytes: number): string {
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < UNITS.length - 1) {
    value /= 1024;
    unit += 1;
  }
  const text = unit === 0 ? String(value) : value.toFixed(1);
  return `${text} ${UNITS[unit] ?? 'B'}`;
}

/** Lower-case extension without the dot ('' when none). */
export function extensionOf(fileName: string): string {
  const dot = fileName.lastIndexOf('.');
  return dot < 0 ? '' : fileName.slice(dot + 1).toLowerCase();
}

/**
 * Client-side pre-check before an upload (the server re-validates everything).
 * Returns an error message, or undefined when the file may be sent.
 */
export function checkFile(
  file: { name: string; size: number },
  allowedExtensions: readonly string[],
  maxBytes: number,
): string | undefined {
  if (file.size === 0) {
    return 'The file is empty.';
  }
  if (file.size > maxBytes) {
    return `The file is larger than ${formatBytes(maxBytes)}.`;
  }
  if (!allowedExtensions.includes(extensionOf(file.name))) {
    return `Only these file types are allowed: ${allowedExtensions.join(', ')}.`;
  }
  return undefined;
}

/**
 * Pre-checks chosen files (type, size, count). Returns the first problem, or undefined when all may be sent.
 */
export function screenFiles(
  files: readonly { name: string; size: number }[],
  extensions: readonly string[],
  maxBytes: number,
  maxFiles: number,
): string | undefined {
  if (files.length > maxFiles) {
    return `Choose at most ${maxFiles} files at a time.`;
  }
  for (const file of files) {
    const error = checkFile(file, extensions, maxBytes);
    if (error !== undefined) {
      return `${file.name}: ${error}`;
    }
  }
  return undefined;
}
