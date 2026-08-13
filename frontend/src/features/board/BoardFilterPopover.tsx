import { useState, useRef, useEffect } from "react";
import { Filter, X, Check, RotateCcw } from "lucide-react";
import type { Priority } from "@/types";

export interface BoardFilterState {
  search: string;
  priorities: Priority[];
  dueDate: "ALL" | "OVERDUE" | "DUE_SOON" | "NO_DUE_DATE";
}

interface BoardFilterPopoverProps {
  filters: BoardFilterState;
  onChange: (filters: BoardFilterState) => void;
  onReset: () => void;
}

const PRIORITIES: Priority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];

export function BoardFilterPopover({ filters, onChange, onReset }: BoardFilterPopoverProps) {
  const [isOpen, setIsOpen] = useState(false);
  const popoverRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (popoverRef.current && !popoverRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const activeCount =
    filters.priorities.length + (filters.dueDate !== "ALL" ? 1 : 0) + (filters.search ? 1 : 0);

  const togglePriority = (p: Priority) => {
    const updated = filters.priorities.includes(p)
      ? filters.priorities.filter((item) => item !== p)
      : [...filters.priorities, p];
    onChange({ ...filters, priorities: updated });
  };

  return (
    <div className="relative inline-block" ref={popoverRef}>
      <button
        onClick={() => setIsOpen((v) => !v)}
        className={`flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-xs font-semibold transition-colors ${
          activeCount > 0
            ? "border-accent-400 bg-accent-50 text-accent-700 dark:bg-accent-950/60 dark:text-accent-300"
            : "border-ink-200 dark:border-ink-700 bg-white dark:bg-ink-800 text-ink-700 dark:text-ink-200 hover:bg-ink-50 dark:hover:bg-ink-700"
        }`}
      >
        <Filter className="h-3.5 w-3.5" />
        <span>Filter</span>
        {activeCount > 0 && (
          <span className="flex h-4 w-4 items-center justify-center rounded-full bg-accent-500 text-[10px] text-white">
            {activeCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-72 rounded-2xl border border-ink-100 dark:border-ink-700 bg-white dark:bg-ink-800 p-4 shadow-xl z-50 animate-in fade-in zoom-in-95 duration-150">
          <div className="flex items-center justify-between border-b border-ink-100 dark:border-ink-700 pb-2 mb-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-ink-900 dark:text-paper">
              Filter Cards
            </h4>
            <div className="flex items-center gap-1">
              {activeCount > 0 && (
                <button
                  onClick={onReset}
                  className="flex items-center gap-1 text-[11px] text-ink-400 hover:text-accent-600 transition-colors"
                  title="Reset filters"
                >
                  <RotateCcw className="h-3 w-3" />
                  <span>Reset</span>
                </button>
              )}
              <button
                onClick={() => setIsOpen(false)}
                className="rounded p-1 text-ink-400 hover:text-ink-600"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          </div>

          <div className="space-y-4">
            {/* Priority Filter */}
            <div>
              <label className="text-[11px] font-bold uppercase tracking-wider text-ink-400 block mb-2">
                Priority
              </label>
              <div className="flex flex-wrap gap-1.5">
                {PRIORITIES.map((p) => {
                  const isSelected = filters.priorities.includes(p);
                  return (
                    <button
                      key={p}
                      onClick={() => togglePriority(p)}
                      className={`flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-medium transition-colors ${
                        isSelected
                          ? "border-accent-500 bg-accent-50 text-accent-700 dark:bg-accent-950/60 dark:text-accent-300"
                          : "border-ink-200 dark:border-ink-700 text-ink-600 dark:text-ink-300 hover:bg-ink-50"
                      }`}
                    >
                      {isSelected && <Check className="h-3 w-3" />}
                      <span>{p.charAt(0) + p.slice(1).toLowerCase()}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Due Date Filter */}
            <div>
              <label className="text-[11px] font-bold uppercase tracking-wider text-ink-400 block mb-2">
                Due Date
              </label>
              <select
                className="input-field text-xs py-1.5"
                value={filters.dueDate}
                onChange={(e) =>
                  onChange({ ...filters, dueDate: e.target.value as BoardFilterState["dueDate"] })
                }
              >
                <option value="ALL">All Cards</option>
                <option value="OVERDUE">Overdue</option>
                <option value="DUE_SOON">Due Soon (Next 7 Days)</option>
                <option value="NO_DUE_DATE">No Due Date</option>
              </select>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
