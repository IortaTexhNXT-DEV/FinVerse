import { useQuery } from '@tanstack/react-query';
import { Upload } from 'lucide-react';
import { useRef, useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import { Button } from '@/components/ui/Button';
import { checkFile, formatBytes } from '@/utils/files';

const DEFAULT_EXTENSIONS = ['pdf', 'png', 'jpg', 'jpeg', 'xlsx', 'csv', 'docx'];
const DEFAULT_MAX_BYTES = 10 * 1024 * 1024;

interface Props {
  /** Unique suffix for input ids. */
  idSuffix: string;
  busy: boolean;
  onUpload: (file: File, description: string) => void;
}

/** Description input, file picker and client-side pre-check (type and size) for attachments. */
export function AttachmentUploadBar({ idSuffix, busy, onUpload }: Readonly<Props>) {
  const input = useRef<HTMLInputElement>(null);
  const [description, setDescription] = useState('');
  const [problem, setProblem] = useState<string>();
  const policy = useQuery({ queryKey: ['attachment-policy'], queryFn: attachmentsApi.policy });
  const extensions = policy.data?.allowedExtensions.split(', ') ?? DEFAULT_EXTENSIONS;
  const maxBytes = policy.data?.maxSizeBytes ?? DEFAULT_MAX_BYTES;

  const onFile = (file: File | undefined) => {
    if (file !== undefined) {
      const error = checkFile(file, extensions, maxBytes);
      setProblem(error);
      if (error === undefined) {
        onUpload(file, description);
        setDescription('');
      }
    }
    if (input.current) {
      input.current.value = '';
    }
  };

  return (
    <>
      <div className="row" style={{ padding: 'var(--space-3) var(--space-4)' }}>
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
        <label className="visually-hidden" htmlFor={`att-file-${idSuffix}`}>
          File to attach
        </label>
        <input
          id={`att-file-${idSuffix}`}
          ref={input}
          type="file"
          className="visually-hidden"
          accept={extensions.map((e) => `.${e}`).join(',')}
          onChange={(e) => onFile(e.target.files?.[0])}
        />
        <Button
          variant="secondary"
          size="sm"
          icon={<Upload size={14} />}
          busy={busy}
          onClick={() => input.current?.click()}
        >
          Attach file
        </Button>
        <span className="muted">
          {extensions.join(', ')} · max {formatBytes(maxBytes)}
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
