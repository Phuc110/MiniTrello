import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { Check } from "lucide-react";
import clsx from "clsx";
import { tagApi } from "@/api/tags";
import type { TagDto } from "@/types";

const PALETTE = [
  { name: "Red", value: "#ef4444" },
  { name: "Green", value: "#10b981" },
  { name: "Blue", value: "#3b82f6" },
  { name: "Orange", value: "#f97316" },
  { name: "Purple", value: "#8b5cf6" },
  { name: "Yellow", value: "#eab308" },
];

interface TagPickerPopoverProps {
  workspaceId: string;
  availableTags: TagDto[];
  isLoadingTags: boolean;
  selectedTags: TagDto[];
  /** Toggle an existing tag on/off the current task (instant API sync). */
  onToggle: (tagId: string) => void;
  /** Called right after a brand-new tag is created — parent auto-attaches it to the task. */
  onCreated: (tag: TagDto) => void;
}

export function TagPickerPopover({
  workspaceId,
  availableTags,
  isLoadingTags,
  selectedTags,
  onToggle,
  onCreated,
}: TagPickerPopoverProps) {
  const queryClient = useQueryClient();
  const [newTagName, setNewTagName] = useState("");
  const [newTagColor, setNewTagColor] = useState(PALETTE[1].value);

  const createTagMutation = useMutation({
    mutationFn: (payload: { name: string; color: string }) =>
      tagApi.create(workspaceId, payload),
    onSuccess: (createdTag) => {
      void queryClient.invalidateQueries({ queryKey: ["workspace-tags", workspaceId] });
      setNewTagName("");
      setNewTagColor(PALETTE[1].value);
      toast.success(`Tag "${createdTag.name}" created`);
      onCreated(createdTag);
    },
    onError: () => toast.error("Could not create the tag. Please try again."),
  });

  const trimmedName = newTagName.trim();

  function handleCreate() {
    if (!trimmedName) return;
    createTagMutation.mutate({ name: trimmedName, color: newTagColor });
  }

  return (
    <div className="w-full space-y-3">
      {/* Column 1 — Create New Custom Tag */}
      <div className="space-y-2 border-b border-ink-100 pb-3 dark:border-ink-700">
        <p className="text-[11px] font-semibold uppercase tracking-wider text-ink-400">
          Create New Tag
        </p>
        <input
          type="text"
          placeholder="Create new tag name..."
          value={newTagName}
          onChange={(e) => setNewTagName(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              handleCreate();
            }
          }}
          className="w-full rounded-md border border-ink-200 bg-white px-2.5 py-1.5 text-xs text-ink-900 placeholder:text-ink-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent dark:border-ink-600 dark:bg-ink-900 dark:text-paper"
        />

        {/* Color palette + Create button on one row */}
        <div className="flex items-center justify-between pt-0.5">
          <div className="flex items-center gap-1.5">
            {PALETTE.map((c) => (
              <button
                key={c.value}
                type="button"
                title={c.name}
                onClick={() => setNewTagColor(c.value)}
                className={clsx(
                  "h-4 w-4 flex-shrink-0 rounded-full transition-transform",
                  newTagColor === c.value
                    ? "scale-110 ring-2 ring-offset-1 ring-gray-600 dark:ring-paper dark:ring-offset-ink-800"
                    : "hover:scale-110"
                )}
                style={{ backgroundColor: c.value }}
                aria-label={`Use color ${c.name}`}
              />
            ))}
          </div>
          <button
            type="button"
            onClick={handleCreate}
            disabled={!trimmedName || createTagMutation.isPending}
            className="rounded-md bg-blue-600 px-2.5 py-1 text-xs font-semibold text-white shadow-sm transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {createTagMutation.isPending ? "Creating..." : "+ Create"}
          </button>
        </div>
      </div>

      {/* Column 2 — Existing workspace tags, click to toggle onto the task */}
      <div>
        <p className="mb-1.5 text-[11px] font-semibold uppercase tracking-wider text-ink-400">
          Workspace Tags
        </p>
        <div className="scrollbar-thin max-h-36 space-y-1 overflow-y-auto">
          {isLoadingTags && (
            <div className="px-2 py-1.5 text-[11px] italic text-ink-400">Loading tags...</div>
          )}
          {!isLoadingTags && availableTags.length === 0 && (
            <div className="px-2 py-1.5 text-[11px] italic text-ink-400">
              No tags yet — create one above
            </div>
          )}
          {availableTags.map((t) => {
            const isApplied = selectedTags.some((x) => x.id === t.id);
            return (
              <button
                key={t.id}
                type="button"
                onClick={() => onToggle(t.id)}
                className="flex w-full items-center justify-between rounded-md px-2 py-1.5 text-left text-xs transition-colors hover:bg-gray-100 dark:hover:bg-ink-700"
              >
                <span className="flex items-center gap-2">
                  <span
                    className="h-2.5 w-2.5 flex-shrink-0 rounded-full"
                    style={{ backgroundColor: t.color }}
                  />
                  <span className="font-medium text-ink-800 dark:text-ink-200">{t.name}</span>
                </span>
                {isApplied && (
                  <span className="font-bold text-blue-600 dark:text-blue-400">
                    <Check className="h-4 w-4" />
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
