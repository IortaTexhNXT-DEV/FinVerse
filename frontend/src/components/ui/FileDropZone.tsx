import { FileUp, X } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { formatBytes } from '@/utils/files';
import { acceptedTypesText, acceptsFile } from '@/utils/presentation';

/** Largest upload the server accepts (BROKERVERSE_UPLOAD_MAX_FILE_SIZE default). */
const MAX_UPLOAD_MB = 20;

interface FileDropZoneProps {
  /** Id of the hidden input, for the field label. */
  id?: string;
  /** Accepted types, as for a file input (".xlsx,.csv", "application/pdf"). */
  accept?: string;
  multiple?: boolean;
  /** Largest file in MB, shown in the hint and checked before the upload. */
  maxSizeMb?: number;
  /** Called with the selected files after every change (empty when all are removed). */
  onChange: (files: File[]) => void;
  disabled?: boolean;
  /** Upload in progress: shows the progress bar (a percentage, or busy without one). */
  busy?: boolean;
  progress?: number;
  /** Accessible name when the zone is not inside a labelled field. */
  label?: string;
}

/** Why a set of picked files is refused, or undefined when all may be sent. */
function refusal(picked: File[], accept: string | undefined, maxSizeMb: number) {
  const wrongType = picked.find((f) => !acceptsFile(f, accept));
  if (wrongType !== undefined) {
    return `${wrongType.name} is not an accepted file type.`;
  }
  const tooLarge = picked.find((f) => f.size > maxSizeMb * 1024 * 1024);
  if (tooLarge !== undefined) {
    return `${tooLarge.name} is larger than ${String(maxSizeMb)} MB.`;
  }
  return undefined;
}

function zoneClass(dragging: boolean, invalid: boolean, disabled: boolean): string {
  return [
    'dropzone',
    dragging ? 'dragging' : '',
    invalid ? 'invalid' : '',
    disabled ? 'disabled' : '',
  ]
    .filter((c) => c !== '')
    .join(' ');
}

function FileList({
  files,
  busy,
  onRemove,
}: Readonly<{ files: File[]; busy: boolean; onRemove: (index: number) => void }>) {
  if (files.length === 0) {
    return null;
  }
  return (
    <ul className="dropzone-files" aria-label="Selected files">
      {files.map((f, i) => (
        <li key={`${f.name}-${String(i)}`} className="dropzone-file">
          <span className="dropzone-file-name" title={f.name}>
            {f.name}
          </span>
          <span className="muted">{formatBytes(f.size)}</span>
          <button
            type="button"
            aria-label={`Remove ${f.name}`}
            disabled={busy}
            onClick={() => onRemove(i)}
          >
            <X size={14} aria-hidden="true" />
          </button>
        </li>
      ))}
    </ul>
  );
}

function Progress({ progress }: Readonly<{ progress?: number }>) {
  const known = progress !== undefined;
  return (
    <div
      className={known ? 'dropzone-progress' : 'dropzone-progress indeterminate'}
      role="progressbar"
      aria-label="Uploading"
      aria-valuenow={progress}
      aria-valuemin={0}
      aria-valuemax={100}
    >
      <div style={known ? { width: `${String(progress)}%` } : undefined} />
    </div>
  );
}

/**
 * The one file picker of BIBS (BDO): a drop zone with drag and drop and a Browse button, the
 * accepted types and maximum size as a single hint, the selected files listed with their size and
 * a remove icon, and a progress state while uploading. Replaces the native "Choose File" input on
 * every upload screen.
 */
export function FileDropZone({
  id,
  accept,
  multiple = false,
  maxSizeMb = MAX_UPLOAD_MB,
  onChange,
  disabled = false,
  busy = false,
  progress,
  label,
}: Readonly<FileDropZoneProps>) {
  const input = useRef<HTMLInputElement>(null);
  const [files, setFiles] = useState<File[]>([]);
  const [dragging, setDragging] = useState(false);
  const [error, setError] = useState<string>();
  const locked = disabled || busy;

  const update = (next: File[]) => {
    setFiles(next);
    onChange(next);
  };

  const choose = (list: FileList | File[] | null) => {
    const picked = Array.from(list ?? []);
    const refused = refusal(picked, accept, maxSizeMb);
    setError(refused);
    if (refused === undefined) {
      update(multiple ? [...files, ...picked] : picked.slice(0, 1));
    }
  };

  const remove = (index: number) => {
    update(files.filter((_, i) => i !== index));
    if (input.current !== null) {
      input.current.value = '';
    }
  };

  const zone = useRef<HTMLElement>(null);
  const chooseRef = useRef(choose);
  useEffect(() => {
    chooseRef.current = choose;
  });
  useEffect(() => {
    const el = zone.current;
    if (el === null) {
      return undefined;
    }
    const over = (e: DragEvent) => {
      e.preventDefault();
      setDragging(!locked);
    };
    const leave = () => setDragging(false);
    const drop = (e: DragEvent) => {
      e.preventDefault();
      setDragging(false);
      if (!locked) {
        chooseRef.current(e.dataTransfer?.files ?? null);
      }
    };
    el.addEventListener('dragover', over);
    el.addEventListener('dragleave', leave);
    el.addEventListener('drop', drop);
    return () => {
      el.removeEventListener('dragover', over);
      el.removeEventListener('dragleave', leave);
      el.removeEventListener('drop', drop);
    };
  }, [locked]);

  const hint = [acceptedTypesText(accept), `max ${String(maxSizeMb)} MB`]
    .filter((t) => t !== '')
    .join(' · ');
  return (
    <div className="dropzone-field">
      <section
        ref={zone}
        className={zoneClass(dragging, error !== undefined, disabled)}
        aria-label={label ?? 'File drop zone'}
      >
        <FileUp size={24} aria-hidden="true" />
        <span className="dropzone-text">
          <span>
            Drag and drop {multiple ? 'files' : 'a file'} here, or{' '}
            <button
              type="button"
              className="link-button inline"
              disabled={locked}
              onClick={() => input.current?.click()}
            >
              Browse
            </button>
          </span>
          <span className="dropzone-hint">{hint}</span>
        </span>
        <input
          ref={input}
          id={id}
          type="file"
          className="visually-hidden"
          accept={accept}
          multiple={multiple}
          disabled={locked}
          aria-label={label}
          aria-invalid={error === undefined ? undefined : true}
          onChange={(e) => choose(e.target.files)}
          tabIndex={-1}
        />
      </section>
      {error !== undefined && (
        <span className="field-error" role="alert">
          {error}
        </span>
      )}
      <FileList files={files} busy={busy} onRemove={remove} />
      {busy && <Progress progress={progress} />}
    </div>
  );
}
