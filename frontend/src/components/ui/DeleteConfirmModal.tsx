import { useState } from "react";
import { AlertTriangle } from "lucide-react";
import { Modal } from "./Modal";
import { Button } from "./Button";
import { Input } from "./Input";

interface DeleteConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  isPending: boolean;
  entityType: "workspace" | "board";
  entityName: string;
}

export function DeleteConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  isPending,
  entityType,
  entityName,
}: DeleteConfirmModalProps) {
  const [confirmText, setConfirmText] = useState("");

  const isMatch = confirmText.trim().toLowerCase() === entityName.trim().toLowerCase();
  const label = entityType === "workspace" ? "Workspace" : "Board";

  function handleClose() {
    setConfirmText("");
    onClose();
  }

  function handleConfirm() {
    if (!isMatch) return;
    onConfirm();
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title={
        <span className="flex items-center gap-2 text-red-600 dark:text-red-400">
          <AlertTriangle className="h-5 w-5 flex-shrink-0" />
          Delete {label}
        </span>
      }
      subtitle={`This action cannot be undone.`}
      size="sm"
    >
      <div className="space-y-4">
        <div className="rounded-xl border border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-950/30 p-4 text-sm text-red-700 dark:text-red-300 leading-relaxed">
          Deleting <strong className="font-semibold">{entityName}</strong> will permanently remove
          this {entityType} and{" "}
          {entityType === "workspace"
            ? "all boards, lists, and tasks inside it"
            : "all lists and tasks inside it"}
          . Members will lose access immediately.
        </div>

        <div>
          <div className="flex items-center justify-between mb-2">
            <p className="text-sm text-ink-600 dark:text-ink-400">
              Type{" "}
              <code className="rounded bg-ink-100 dark:bg-ink-700 px-1.5 py-0.5 font-mono text-xs text-ink-800 dark:text-ink-200">
                {entityName}
              </code>{" "}
              to confirm:
            </p>
            <button
              type="button"
              onClick={() => setConfirmText(entityName)}
              className="text-[11px] font-semibold text-accent-600 dark:text-accent-400 hover:underline"
            >
              Auto-fill
            </button>
          </div>
          <Input
            label="Confirm name"
            placeholder={entityName}
            value={confirmText}
            onChange={(e) => setConfirmText(e.target.value)}
            autoFocus
            onKeyDown={(e) => {
              if (e.key === "Enter" && isMatch) handleConfirm();
            }}
          />
        </div>

        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="secondary" onClick={handleClose} disabled={isPending}>
            Cancel
          </Button>
          <button
            id={`confirm-delete-${entityType}`}
            disabled={!isMatch || isPending}
            onClick={handleConfirm}
            className="inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold bg-red-600 text-white shadow-sm hover:bg-red-700 active:bg-red-800 disabled:opacity-40 disabled:cursor-not-allowed transition-all duration-150"
          >
            {isPending ? (
              <span className="flex items-center gap-2">
                <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                </svg>
                Deleting\u2026
              </span>
            ) : (
              `Delete ${label}`
            )}
          </button>
        </div>
      </div>
    </Modal>
  );
}
