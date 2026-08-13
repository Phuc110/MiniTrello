import { forwardRef, type ButtonHTMLAttributes } from "react";
import clsx from "clsx";
import { Loader2 } from "lucide-react";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "danger";
  isLoading?: boolean;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = "primary", isLoading, disabled, className, children, ...props }, ref) => {
    const base =
      variant === "secondary"
        ? "btn-secondary"
        : variant === "danger"
          ? "inline-flex items-center justify-center gap-2 rounded-card bg-priority-urgent px-4 py-2 text-sm font-medium text-white transition-colors hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
          : "btn-primary";

    return (
      <button ref={ref} className={clsx(base, className)} disabled={disabled || isLoading} {...props}>
        {isLoading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
        {children}
      </button>
    );
  },
);
Button.displayName = "Button";
