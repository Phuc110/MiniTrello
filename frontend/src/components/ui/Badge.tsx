import clsx from "clsx";
import type { Priority, ProjectRole } from "@/types";

interface BadgeProps {
  children: React.ReactNode;
  variant?: "default" | "priority" | "role" | "accent" | "outline";
  priority?: Priority;
  role?: ProjectRole;
  className?: string;
}

const PRIORITY_STYLES: Record<Priority, string> = {
  LOW: "bg-ink-100 text-ink-700 dark:bg-ink-700 dark:text-ink-200 border-ink-200 dark:border-ink-600",
  MEDIUM: "bg-blue-50 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300 border-blue-200 dark:border-blue-800",
  HIGH: "bg-amber-50 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300 border-amber-200 dark:border-amber-800",
  URGENT: "bg-red-50 text-red-700 dark:bg-red-900/30 dark:text-red-300 border-red-200 dark:border-red-800",
};

const ROLE_STYLES: Record<ProjectRole, string> = {
  OWNER: "bg-purple-50 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300 border-purple-200 dark:border-purple-800",
  MANAGER: "bg-indigo-50 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300 border-indigo-200 dark:border-indigo-800",
  CONTRIBUTOR: "bg-blue-50 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300 border-blue-200 dark:border-blue-800",
  VIEWER: "bg-ink-50 text-ink-600 dark:bg-ink-800 dark:text-ink-300 border-ink-200 dark:border-ink-700",
};

export function Badge({
  children,
  variant = "default",
  priority,
  role,
  className,
}: BadgeProps) {
  let styleClass =
    "inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-medium tracking-wide transition-colors";

  if (variant === "priority" && priority) {
    styleClass = clsx(styleClass, PRIORITY_STYLES[priority]);
  } else if (variant === "role" && role) {
    styleClass = clsx(styleClass, ROLE_STYLES[role]);
  } else if (variant === "accent") {
    styleClass = clsx(
      styleClass,
      "bg-accent-50 text-accent-700 border-accent-200 dark:bg-accent-900/30 dark:text-accent-300 dark:border-accent-800"
    );
  } else if (variant === "outline") {
    styleClass = clsx(
      styleClass,
      "bg-transparent text-ink-600 dark:text-ink-300 border-ink-200 dark:border-ink-700"
    );
  } else {
    styleClass = clsx(
      styleClass,
      "bg-ink-100 text-ink-800 dark:bg-ink-700 dark:text-ink-200 border-transparent"
    );
  }

  return <span className={clsx(styleClass, className)}>{children}</span>;
}
