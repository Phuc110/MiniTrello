import { useState, useRef, useEffect } from "react";
import { Filter, X, Check, RotateCcw, Tag } from "lucide-react";
import clsx from "clsx";
import type { Priority, TagDto } from "@/types";

export interface BoardFilterState {
  search: string;
  priorities: Priority[];
  dueDate: "ALL" | "OVERDUE" | "DUE_SOON" | "NO_DUE_DATE";
  tagIds: string[];
}

interface BoardFilterPopoverProps {
  filters: BoardFilterState;
  onChange: (filters: BoardFilterState) => void;
  onReset: () => void;
  availableTags: TagDto[];
}

const PRIORITIES: Priority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];

export function BoardFilterPopover({ filters, onChange, onReset, availableTags }: BoardFilterPopoverProps) {
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
    filters.priorities.length +
    (filters.dueDate !== "ALL" ? 1 : 0) +
    (filters.search ? 1 : 0) +
    filters.tagIds.length;

  const togglePriority = (p: Priority) => {
    const updated = filters.priorities.includes(p)
      ? filters.priorities.filter((item) => item !== p)
      : [...filters.priorities, p];
    onChange({ ...filters, priorities: updated });
  };

  const toggleTag = (tagId: string) => {
    const updated = filters.tagIds.includes(tagId)
      ? filters.tagIds.filter((id) => id !== tagId)
      : [...filters.tagIds, tagId];
    onChange({ ...filters, tagIds: updated });
  };

  return (
    <div className="relative inline-block" ref={popoverRef}>
      <button
        onClick={() => setIsOpen((v) => !v)}
        className={`flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-xs font-semibold transition-colors ${
          activeCount > 0
            ? "border-blue-500 bg-blue-50 text-blue-600 dark:bg-blue-950/60 dark:text-blue-300"
            : "border-ink-200 dark:border-ink-700 bg-white dark:bg-ink-800 text-ink-700 dark:text-ink-200 hover:bg-ink-50 dark:hover:bg-ink-700"
        }`}
      >
        <Filter className="h-3.5 w-3.5" />
        <span>Filter{activeCount > 0 ? ` (${activeCount})` : ""}</span>
      </button>

      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-72 rounded-2xl border border-ink-100 dark:border-ink-700 bg-white dark:bg-ink-800 p-4 shadow-xl z-50 animate-in fade-in zoom-in-95 duration-150">
          <div className="flex items-center justify-between border-b border-ink-100 dark:border-ink-700 pb-2 mb-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-ink-900 dark:text-paper">
              Filter Cards
            </h4>
            <button
              onClick={() => setIsOpen(false)}
              className="rounded p-1 text-ink-400 hover:text-ink-600"
            >
              <X className="h-4 w-4" />
            </button>
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

            {/* Tags Filter */}
            <div>
              <label className="flex items-center justify-between text-[11px] font-bold uppercase tracking-wider text-ink-400 mb-2">
                <span>Tags</span>
                {filters.tagIds.length > 0 && (
                  <span className="font-semibold normal-case text-blue-600 dark:text-blue-400">
                    {filters.tagIds.length} selected
                  </span>
                )}
              </label>

              {availableTags.length === 0 ? (
                <p className="rounded-md bg-ink-50 px-2 py-1.5 text-[11px] italic text-ink-400 dark:bg-ink-900">
                  No tags in this workspace yet
                </p>
              ) : (
                <div className="scrollbar-thin max-h-36 space-y-0.5 overflow-y-auto pr-1">
                  {availableTags.map((tag) => {
                    const isChecked = filters.tagIds.includes(tag.id);
                    return (
                      <button
                        key={tag.id}
                        onClick={() => toggleTag(tag.id)}
                        className="flex w-full items-center justify-between rounded-md px-2 py-1.5 text-left text-xs transition-colors hover:bg-gray-100 dark:hover:bg-ink-700"
                        aria-pressed={isChecked}
                      >
                        <span className="flex items-center gap-2">
                          <span
                            className={clsx(
                              "flex h-4 w-4 flex-shrink-0 items-center justify-center rounded border transition-colors",
                              isChecked
                                ? "border-blue-500 bg-blue-600 text-white"
                                : "border-ink-300 bg-white dark:border-ink-600 dark:bg-ink-900"
                            )}
                          >
                            {isChecked && <Check className="h-3 w-3" strokeWidth={3} />}
                          </span>
                          <span
                            className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold text-white shadow-sm"
                            style={{ backgroundColor: tag.color || "#10b981" }}
                          >
                            <Tag className="h-2.5 w-2.5" />
                            {tag.name}
                          </span>
                        </span>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          {/* Clear all filters */}
          <div className="mt-4 border-t border-ink-100 pt-3 dark:border-ink-700">
            <button
              onClick={onReset}
              disabled={activeCount === 0}
              className="flex w-full items-center justify-center gap-1.5 rounded-lg border border-ink-200 py-1.5 text-xs font-semibold text-ink-600 transition-colors hover:border-red-300 hover:bg-red-50 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-40 dark:border-ink-700 dark:text-ink-300 dark:hover:border-red-500/40 dark:hover:bg-red-950/30 dark:hover:text-red-400"
            >
              <RotateCcw className="h-3 w-3" />
              <span>Clear filters</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
