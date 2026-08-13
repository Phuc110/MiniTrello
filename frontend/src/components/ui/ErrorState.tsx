import { AlertTriangle } from "lucide-react";
import { Button } from "./Button";

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div
      role="alert"
      className="flex flex-col items-center justify-center gap-3 rounded-card border border-priority-urgent/30 bg-priority-urgent/5 px-6 py-14 text-center"
    >
      <AlertTriangle className="h-8 w-8 text-priority-urgent" aria-hidden="true" />
      <p className="text-sm text-ink-600 dark:text-ink-200">{message}</p>
      {onRetry && (
        <Button variant="secondary" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  );
}
