import { AlertTriangle, RefreshCw } from 'lucide-react';
import { Component, type ReactNode } from 'react';
import { isChunkLoadError } from '@/app/loadRecovery';
import { Button } from './Button';

const reloadPage = () => window.location.reload();

/** The message shown instead of a page that could not be loaded or shown, with a Reload action. */
export function LoadErrorPage({
  newVersion,
  title,
  text,
  onReload = reloadPage,
}: Readonly<{ newVersion?: boolean; title?: string; text?: string; onReload?: () => void }>) {
  return (
    <div className="status-page" role="alert">
      <AlertTriangle size={48} aria-hidden="true" />
      <h1>{title ?? 'Something went wrong loading this page.'}</h1>
      <p>
        {text ??
          (newVersion
            ? 'A new version of BIBS is available. Reload to continue.'
            : 'Reload the page to try again. If the problem continues, contact the BIBS support team.')}
      </p>
      <div className="row">
        <Button onClick={onReload}>
          <RefreshCw size={16} aria-hidden="true" /> Reload
        </Button>
      </div>
    </div>
  );
}

interface Props {
  readonly children: ReactNode;
  readonly resetKey?: string;
  readonly onReload?: () => void;
}
interface State {
  error: unknown;
}

/**
 * Catches errors of the pages below it (a screen that failed to load included) and shows
 * {@link LoadErrorPage} instead of a blank page. A change of resetKey (the route) clears the error.
 */
export class LoadErrorBoundary extends Component<Props, State> {
  override state: State = { error: null };

  static getDerivedStateFromError(error: unknown): State {
    return { error: error ?? new Error('Unknown error') };
  }

  override componentDidUpdate(previous: Props) {
    if (this.state.error !== null && previous.resetKey !== this.props.resetKey) {
      this.setState({ error: null });
    }
  }

  override render(): ReactNode {
    if (this.state.error !== null) {
      return (
        <LoadErrorPage
          newVersion={isChunkLoadError(this.state.error)}
          onReload={this.props.onReload}
        />
      );
    }
    return <>{this.props.children}</>;
  }
}
