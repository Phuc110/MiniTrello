import clsx from "clsx";

export function Skeleton({ className }: { className?: string }) {
  return (
    <div
      className={clsx("animate-pulse rounded-card bg-ink-100 dark:bg-ink-700", className)}
      aria-hidden="true"
    />
  );
}
