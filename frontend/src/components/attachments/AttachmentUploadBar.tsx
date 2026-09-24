import { useQuery } from '@tanstack/react-query';
import { Upload } from 'lucide-react';
import { useRef, useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import type { UploadOptions } from '@/api/attachments';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { formatBytes, screenFiles } from '@/utils/files';

const DEFAULT_EXTENSIONS = ['pdf', 'png', 'jpg', 'jpeg', 'xlsx', 'csv', 'docx'];
const DEFAULT_MAX_BYTES = 10 * 1024 * 1024;
const DEFAULT_MAX_FILES = 50;

interface Props {
  /** Unique suffix for input ids. */
  idSuffix: string;
  busy: boolean;
  /** Show the document type list (DOCUMENT_TYPE). */
  documentTypes?: boolean;
  /** Business reference used to nominate file names; enables the naming choice. */
  reference?: string;
  onUpload: (files: File[], options: UploadOptions) => void;
}

interface NamingProps {
  reference: string;
  documentType: string;
  syntax?: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}

/** Keep the original file names, or name the files after the record and document type. */
function NamingChoice({
  reference,
  documentType,
  syntax,
  checked,
  onChange,
}: Readonly<NamingProps>) {
  return (
    <label className="checkbox" title={syntax}>
      <input
        type="checkbox"
        checked={checked}
        disabled={documentType === ''}
        onChange={(e) => onChange(e.target.checked)}
      />
      Name as {reference}_{documentType === '' ? 'TYPE' : documentType}_n
    </label>
  );
}

/**
 * Description, document type, naming choice, file picker (one or several files) and the
 * client-side pre-check (type, size, count) for attachments. The server re-validates everything.
 */
export function AttachmentUploadBar({
  idSuffix,
  busy,
  documentTypes = true,
  reference,
  onUpload,
}: Readonly<Props>) {
  const input = useRef<HTMLInputElement>(null);
  const [description, setDescription] = useState('');
  const [documentType, setDocumentType] = useState('');
  const [nominate, setNominate] = useState(false);
  const [problem, setProblem] = useState<string>();
  const policy = useQuery({ queryKey: ['attachment-policy'], queryFn: attachmentsApi.policy });
  const extensions = policy.data?.allowedExtensions.split(', ') ?? DEFAULT_EXTENSIONS;
  const maxBytes = policy.data?.maxSizeBytes ?? DEFAULT_MAX_BYTES;
  const maxFiles = policy.data?.maxFiles ?? DEFAULT_MAX_FILES;

  const onFiles = (list: FileList | null) => {
    const files = list === null ? [] : Array.from(list);
    if (files.length > 0) {
      const error = screenFiles(files, extensions, maxBytes, maxFiles);
      setProblem(error);
      if (error === undefined) {
        onUpload(files, {
          description,
          documentType,
          naming: nominate ? 'NOMINATE' : 'INHERIT',
          reference,
        });
        setDescription('');
      }
    }
    if (input.current) {
      input.current.value = '';
    }
  };

  return (
    <>
      <div className="row" style={{ padding: 'var(--space-3) var(--space-4)', flexWrap: 'wrap' }}>
        {documentTypes && (
          <>
            <label className="visually-hidden" htmlFor={`att-type-${idSuffix}`}>
              Document type
            </label>
            <LovSelect
              id={`att-type-${idSuffix}`}
              type="DOCUMENT_TYPE"
              value={documentType}
              placeholder="Document type (optional)"
              onChange={setDocumentType}
            />
          </>
        )}
        <label className="visually-hidden" htmlFor={`att-desc-${idSuffix}`}>
          Description
        </label>
        <input
          id={`att-desc-${idSuffix}`}
          className="input"
          placeholder="Description (optional)"
          maxLength={200}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        {reference !== undefined && (
          <NamingChoice
            reference={reference}
            documentType={documentType}
            syntax={policy.data?.namingSyntax}
            checked={nominate}
            onChange={setNominate}
          />
        )}
        <label className="visually-hidden" htmlFor={`att-file-${idSuffix}`}>
          Files to attach
        </label>
        <input
          id={`att-file-${idSuffix}`}
          ref={input}
          type="file"
          multiple
          className="visually-hidden"
          accept={extensions.map((e) => `.${e}`).join(',')}
          onChange={(e) => onFiles(e.target.files)}
        />
        <Button
          variant="secondary"
          size="sm"
          icon={<Upload size={14} />}
          busy={busy}
          onClick={() => input.current?.click()}
        >
          Attach files
        </Button>
        <span className="muted">
          {extensions.join(', ')} · max {formatBytes(maxBytes)} · up to {maxFiles} files
        </span>
      </div>
      {problem !== undefined && (
        <div className="alert warning" role="alert">
          {problem}
        </div>
      )}
    </>
  );
}
