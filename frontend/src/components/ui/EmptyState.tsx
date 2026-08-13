import type { ReactNode } from "react";

export function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-card border border-dashed border-ink-200 dark:border-ink-600 px-6 py-14 text-center">
      {icon && <div className="text-ink-300">{icon}</div>}
      <div>
        <p className="font-display text-base font-semibold text-ink-700 dark:text-paper">{title}</p>
        {description && <p className="mt-1 text-sm text-ink-400">{description}</p>}
      </div>
      {action}
    </div>
  );
}
