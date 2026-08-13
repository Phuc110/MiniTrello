import { forwardRef, type InputHTMLAttributes } from "react";
import clsx from "clsx";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, id, className, ...props }, ref) => {
    const inputId = id ?? label.toLowerCase().replace(/\s+/g, "-");
    return (
      <div className="flex flex-col gap-1.5">
        <label htmlFor={inputId} className="text-sm font-medium text-ink-700 dark:text-ink-200">
          {label}
        </label>
        <input
          ref={ref}
          id={inputId}
          className={clsx("input-field", error && "border-priority-urgent", className)}
          aria-invalid={!!error}
          aria-describedby={error ? `${inputId}-error` : undefined}
          {...props}
        />
        {error && (
          <p id={`${inputId}-error`} role="alert" className="text-sm text-priority-urgent">
            {error}
          </p>
        )}
      </div>
    );
  },
);
Input.displayName = "Input";
