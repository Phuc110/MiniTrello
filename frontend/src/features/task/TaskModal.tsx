import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Rnd } from "react-rnd";
import toast from "react-hot-toast";
import {
  Trash2,
  AlignLeft,
  Users,
  Check,
  Plus,
  Sparkles,
  X,
} from "lucide-react";
import { taskSchema, type TaskFormValues } from "@/lib/validation";
import { taskApi } from "@/api/tasks";
import { workspaceApi } from "@/api/workspaces";
import { tagApi } from "@/api/tags";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Avatar } from "@/components/ui/Avatar";
import { TagPickerPopover } from "./TagPickerPopover";
import type { Task, Priority, TagDto } from "@/types";

const PRIORITIES: Priority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];

const DEFAULT_WIDTH = Math.min(850, window.innerWidth - 32);
const DEFAULT_HEIGHT = Math.min(550, window.innerHeight - 32);

interface TaskModalProps {
  task: Task;
  workspaceId?: string;
  onClose: () => void;
}

export function TaskModal({ task, workspaceId, onClose }: TaskModalProps) {
  const queryClient = useQueryClient();
  const [isAssigneePopoverOpen, setIsAssigneePopoverOpen] = useState(false);
  const [isTagPickerOpen, setIsTagPickerOpen] = useState(false);
  const tagPickerRef = useRef<HTMLDivElement>(null);

  // Callers opening the modal from outside a board (e.g. My Tasks in the
  // sidebar) may not know the workspace — fall back to the one resolved by
  // the backend on the task payload itself.
  const resolvedWorkspaceId = workspaceId ?? task.workspaceId ?? undefined;

  // `task` is a snapshot prop from the board — keep a local copy of tags so
  // toggles reflect instantly without closing/reopening the window.
  // Callers MUST pass key={task.id}: per-task state (tags below plus the
  // react-hook-form defaults) then re-initializes whenever another task is
  // loaded into this window, keeping everything in sync with the new task.
  const [taskTags, setTaskTags] = useState<TagDto[]>(task.tags ?? []);

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  useEffect(() => {
    if (!isTagPickerOpen) return;
    function handleMouseDown(e: MouseEvent) {
      if (tagPickerRef.current && !tagPickerRef.current.contains(e.target as Node)) {
        setIsTagPickerOpen(false);
      }
    }
    document.addEventListener("mousedown", handleMouseDown);
    return () => document.removeEventListener("mousedown", handleMouseDown);
  }, [isTagPickerOpen]);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<TaskFormValues>({
    resolver: zodResolver(taskSchema),
    defaultValues: {
      title: task.title,
      description: task.description ?? "",
      priority: task.priority,
      dueDate: task.dueDate ?? "",
    },
  });

  const membersQuery = useQuery({
    queryKey: ["workspace-members", resolvedWorkspaceId],
    queryFn: () => workspaceApi.listMembers(resolvedWorkspaceId!),
    enabled: !!resolvedWorkspaceId,
  });

  const tagsQuery = useQuery({
    queryKey: ["workspace-tags", resolvedWorkspaceId],
    queryFn: () => tagApi.listByWorkspace(resolvedWorkspaceId!),
    enabled: !!resolvedWorkspaceId,
  });

  const updateMutation = useMutation({
    mutationFn: (values: TaskFormValues) =>
      taskApi.update(task.id, {
        title: values.title,
        description: values.description,
        priority: values.priority,
        dueDate: values.dueDate || null,
      }),
    onSuccess: () => {
      toast.success("Task updated");
      void queryClient.invalidateQueries({ queryKey: ["tasks", task.boardListId] });
      void queryClient.invalidateQueries({ queryKey: ["my-tasks"] });
      onClose();
    },
    onError: () => toast.error("Couldn't update the task. Please try again."),
  });

  const deleteMutation = useMutation({
    mutationFn: () => taskApi.remove(task.id),
    onSuccess: () => {
      toast.success("Task deleted");
      void queryClient.invalidateQueries({ queryKey: ["tasks", task.boardListId] });
      void queryClient.invalidateQueries({ queryKey: ["my-tasks"] });
      onClose();
    },
    onError: () => toast.error("Couldn't delete the task. Please try again."),
  });

  const assignMutation = useMutation({
    mutationFn: (userId: string) => taskApi.assign(task.id, userId),
    onSuccess: () => {
      toast.success("Assignee updated");
      void queryClient.invalidateQueries({ queryKey: ["tasks", task.boardListId] });
      void queryClient.invalidateQueries({ queryKey: ["my-tasks"] });
    },
    onError: () => toast.error("Could not update assignee."),
  });

  const unassignMutation = useMutation({
    mutationFn: (userId: string) => taskApi.unassign(task.id, userId),
    onSuccess: () => {
      toast.success("Assignee removed");
      void queryClient.invalidateQueries({ queryKey: ["tasks", task.boardListId] });
      void queryClient.invalidateQueries({ queryKey: ["my-tasks"] });
    },
    onError: () => toast.error("Could not remove assignee."),
  });

  const addTagMutation = useMutation({
    mutationFn: (tagId: string) => taskApi.addTag(task.id, tagId),
    onSuccess: (_data, tagId) => {
      const added =
        tagsQuery.data?.find((t) => t.id === tagId) ??
        // brand-new tags arrive via onCreated before the list refetches
        undefined;
      setTaskTags((prev) =>
        added && !prev.some((t) => t.id === tagId) ? [...prev, added] : prev
      );
      void queryClient.invalidateQueries({ queryKey: ["tasks", task.boardListId] });
      void queryClient.invalidateQueries({ queryKey: ["my-tasks"] });
    },
    onError: () => toast.error("Could not add the tag. Please try again."),
  });

  const removeTagMutation = useMutation({
    mutationFn: (tagId: string) => taskApi.removeTag(task.id, tagId),
    onSuccess: (_data, tagId) => {
      setTaskTags((prev) => prev.filter((t) => t.id !== tagId));
      void queryClient.invalidateQueries({ queryKey: ["tasks", task.boardListId] });
      void queryClient.invalidateQueries({ queryKey: ["my-tasks"] });
    },
    onError: () => toast.error("Could not remove the tag. Please try again."),
  });

  const toggleTag = (tagId: string) => {
    const isApplied = taskTags.some((t) => t.id === tagId);
    if (isApplied) {
      removeTagMutation.mutate(tagId);
    } else {
      addTagMutation.mutate(tagId);
    }
  };

  /** Auto-attach a freshly created tag — optimistic local insert so the
   * checkmark shows even before tagsQuery refetches. */
  const handleTagCreated = (created: TagDto) => {
    setTaskTags((prev) => (prev.some((t) => t.id === created.id) ? prev : [...prev, created]));
    addTagMutation.mutate(created.id);
  };

  const toggleAssignee = (userId: string) => {
    const isAssigned = task.assignees.some((a) => a.userId === userId);
    if (isAssigned) {
      unassignMutation.mutate(userId);
    } else {
      assignMutation.mutate(userId);
    }
  };

  // Portal to <body>: ancestors of the board canvas carry transform/backdrop-blur
  // (dnd transforms, blurred header) which trap fixed/z-index in their stacking
  // context — rendering at the document root guarantees top-most layer.
  return createPortal(
    <div className="fixed inset-0 z-[9999] pointer-events-auto flex items-center justify-center bg-black/40 backdrop-blur-sm">
      <Rnd
        bounds="window"
        dragHandleClassName="drag-handle"
        default={{
          x: Math.max(16, (window.innerWidth - DEFAULT_WIDTH) / 2),
          y: Math.max(16, (window.innerHeight - DEFAULT_HEIGHT) / 2),
          width: DEFAULT_WIDTH,
          height: DEFAULT_HEIGHT,
        }}
        minWidth={500}
        minHeight={400}
        maxWidth={1080}
        maxHeight={600}
      >
        <div className="flex h-full w-full flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-2xl dark:border-ink-700 dark:bg-ink-800">
          {/* Title Bar / Drag Handle */}
          <div className="drag-handle flex cursor-move select-none items-center justify-between border-b border-gray-200 bg-gray-50 px-6 py-4 dark:border-ink-700 dark:bg-ink-800/80">
            <div className="flex items-center gap-2 font-semibold text-gray-700 dark:text-paper">
              <Sparkles className="h-5 w-5 text-accent-500" />
              <span>Task Details</span>
              <span className="text-xs font-normal text-gray-400 dark:text-ink-400">
                — drag this bar to move · corners to resize
              </span>
            </div>
            <button
              onClick={onClose}
              className="rounded-lg p-1 text-gray-500 transition-colors hover:bg-gray-200 hover:text-gray-700 dark:text-ink-400 dark:hover:bg-ink-700 dark:hover:text-ink-200"
              aria-label="Close"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Body — scrollable, 2-column grid */}
          <form
            onSubmit={handleSubmit((values) => updateMutation.mutate(values))}
            noValidate
            className="grid flex-1 grid-cols-1 gap-6 overflow-y-auto p-6 md:grid-cols-3"
          >
            {/* Main Content (2/3) */}
            <div className="min-w-0 space-y-4 md:col-span-2">
              <div>
                <input
                  {...register("title")}
                  placeholder="Task title"
                  className="w-full rounded-lg border-2 border-transparent bg-transparent px-2 py-1 text-xl font-bold text-ink-900 placeholder:text-ink-400 transition-colors hover:border-ink-200 focus:border-blue-500 focus:bg-white focus:outline-none dark:text-paper dark:focus:bg-ink-900 dark:hover:border-ink-700"
                />
                {errors.title?.message && (
                  <p className="mt-1 pl-2 text-xs text-red-500">{errors.title.message}</p>
                )}
              </div>

              <div className="space-y-1.5">
                <label className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-ink-700 dark:text-ink-200">
                  <AlignLeft className="h-4 w-4 text-ink-400" />
                  <span>Description</span>
                </label>
                <textarea
                  {...register("description")}
                  rows={6}
                  placeholder="Add a detailed description for this task..."
                  className="w-full min-h-[150px] resize-y rounded-lg border border-ink-200 bg-white p-3 text-sm leading-relaxed text-ink-800 placeholder:text-ink-400 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-ink-600 dark:bg-ink-900 dark:text-paper"
                />
              </div>

              {/* Assigned Members */}
              <div className="space-y-2 pt-2">
                <label className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-ink-700 dark:text-ink-200">
                  <Users className="h-4 w-4 text-ink-400" />
                  <span>Assigned Members</span>
                </label>
                <div className="flex flex-wrap items-center gap-2">
                  {task.assignees.map((a) => (
                    <div
                      key={a.userId}
                      className="flex items-center gap-1.5 rounded-full border border-ink-200 bg-ink-50 pr-2.5 pl-1 py-1 text-xs dark:border-ink-700 dark:bg-ink-800"
                    >
                      <Avatar name={a.fullName} size="sm" />
                      <span className="font-semibold text-ink-800 dark:text-ink-200">
                        {a.fullName}
                      </span>
                    </div>
                  ))}

                  {workspaceId && (
                    <div className="relative">
                      <button
                        type="button"
                        onClick={() => setIsAssigneePopoverOpen((v) => !v)}
                        className="inline-flex items-center gap-1 rounded-full border border-dashed border-ink-300 px-3 py-1 text-xs font-semibold text-accent-600 transition-colors hover:bg-accent-50 dark:border-ink-600 dark:hover:bg-accent-950/40"
                      >
                        <Plus className="h-3.5 w-3.5" />
                        <span>Assign</span>
                      </button>

                      {isAssigneePopoverOpen && (
                        <div className="absolute left-0 top-full z-50 mt-2 w-64 animate-in rounded-xl border border-ink-100 bg-white p-2 shadow-xl fade-in duration-150 dark:border-ink-700 dark:bg-ink-800">
                          <div className="px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-ink-400">
                            Workspace Members
                          </div>
                          <div className="scrollbar-thin mt-1 max-h-40 space-y-0.5 overflow-y-auto">
                            {membersQuery.data?.map((m) => {
                              const isAssigned = task.assignees.some(
                                (a) => a.userId === m.userId
                              );
                              return (
                                <button
                                  key={m.userId}
                                  type="button"
                                  onClick={() => toggleAssignee(m.userId)}
                                  className="flex w-full items-center justify-between rounded-lg px-2.5 py-1.5 text-left text-xs transition-colors hover:bg-ink-50 dark:hover:bg-ink-700"
                                >
                                  <div className="flex items-center gap-2">
                                    <Avatar name={m.fullName} size="sm" />
                                    <span className="font-medium text-ink-800 dark:text-ink-200">
                                      {m.fullName}
                                    </span>
                                  </div>
                                  {isAssigned && (
                                    <Check className="h-4 w-4 text-accent-500" />
                                  )}
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Sidebar Controls (1/3): Task Configuration */}
            <div className="space-y-4 md:col-span-1 md:border-l md:border-ink-100 md:pl-6 dark:md:border-ink-700">
              <h4 className="text-xs font-bold uppercase tracking-wider text-ink-500 dark:text-ink-400">
                Task Configuration
              </h4>

              <div className="space-y-1.5">
                <label className="block text-xs font-semibold text-ink-700 dark:text-ink-200">
                  Priority
                </label>
                <select
                  className="input-field py-2 text-xs"
                  {...register("priority")}
                >
                  {PRIORITIES.map((p) => (
                    <option key={p} value={p}>
                      {p.charAt(0) + p.slice(1).toLowerCase()}
                    </option>
                  ))}
                </select>
              </div>

              <Input
                label="Due Date"
                type="date"
                className="py-2 text-xs"
                error={errors.dueDate?.message}
                {...register("dueDate")}
              />

              {/* Tags — interactive picker */}
              {resolvedWorkspaceId && (
                <div className="space-y-1.5">
                  <label className="flex items-center justify-between text-xs font-bold uppercase tracking-wider text-gray-500 dark:text-ink-400">
                    <span>Tags</span>
                    <span className="text-[11px] font-normal normal-case text-gray-400">
                      {taskTags.length} applied
                    </span>
                  </label>

                  <div className="flex flex-wrap items-center gap-1.5">
                    {taskTags.length === 0 && (
                      <span className="text-[11px] italic text-ink-400">No tags yet</span>
                    )}
                    {taskTags.map((t) => (
                      <span
                        key={t.id}
                        className="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold text-white shadow-sm"
                        style={{ backgroundColor: t.color || "#10b981" }}
                      >
                        {t.name}
                        <button
                          type="button"
                          onClick={() => removeTagMutation.mutate(t.id)}
                          disabled={
                            removeTagMutation.isPending &&
                            removeTagMutation.variables === t.id
                          }
                          className="ml-0.5 rounded-full p-0.5 font-bold transition-opacity hover:opacity-75"
                          aria-label={`Remove tag ${t.name}`}
                        >
                          <X className="h-3 w-3" />
                        </button>
                      </span>
                    ))}
                  </div>

                  <div ref={tagPickerRef} className="relative">
                    <button
                      type="button"
                      onClick={() => setIsTagPickerOpen((v) => !v)}
                      className="mt-2 flex w-full items-center justify-center gap-1.5 rounded-lg border-2 border-dashed border-gray-300 py-1.5 text-xs font-medium text-gray-600 transition-all hover:border-blue-500 hover:text-blue-600 dark:border-ink-600 dark:text-ink-300 dark:hover:border-accent-400 dark:hover:text-accent-400"
                    >
                      <Plus className="h-3.5 w-3.5" />
                      <span>Add / edit tags</span>
                    </button>

                    {isTagPickerOpen && (
                      <div className="absolute left-0 top-full z-[100001] mt-1 w-64 animate-in space-y-3 rounded-xl border border-gray-200 bg-white p-3 shadow-2xl fade-in zoom-in-95 duration-150 dark:border-ink-700 dark:bg-ink-800">
                        <TagPickerPopover
                          workspaceId={resolvedWorkspaceId}
                          availableTags={tagsQuery.data ?? []}
                          isLoadingTags={tagsQuery.isLoading}
                          selectedTags={taskTags}
                          onToggle={toggleTag}
                          onCreated={handleTagCreated}
                        />
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Actions */}
              <div className="space-y-2 border-t border-ink-100 pt-4 dark:border-ink-700">
                <Button
                  type="submit"
                  className="w-full"
                  isLoading={isSubmitting}
                  disabled={!isDirty}
                >
                  Save Changes
                </Button>

                <Button
                  type="button"
                  variant="danger"
                  className="w-full"
                  onClick={() => {
                    if (window.confirm("Delete this task? This action cannot be undone.")) {
                      deleteMutation.mutate();
                    }
                  }}
                  isLoading={deleteMutation.isPending}
                >
                  <Trash2 className="h-4 w-4" />
                  <span>Delete Task</span>
                </Button>
              </div>
            </div>
          </form>
        </div>
      </Rnd>
    </div>,
    document.body
  );
}
