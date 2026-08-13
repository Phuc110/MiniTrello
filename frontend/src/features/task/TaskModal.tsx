import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import {
  Trash2,
  AlignLeft,
  Users,
  Check,
  Plus,
  Sparkles,
} from "lucide-react";
import { taskSchema, type TaskFormValues } from "@/lib/validation";
import { taskApi } from "@/api/tasks";
import { projectApi } from "@/api/projects";
import { Modal } from "@/components/ui/Modal";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Avatar } from "@/components/ui/Avatar";
import type { Task, Priority } from "@/types";

const PRIORITIES: Priority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];

interface TaskModalProps {
  task: Task;
  projectId?: string;
  onClose: () => void;
}

export function TaskModal({ task, projectId, onClose }: TaskModalProps) {
  const queryClient = useQueryClient();
  const [isAssigneePopoverOpen, setIsAssigneePopoverOpen] = useState(false);

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

  // Fetch project members for assignee selection
  const membersQuery = useQuery({
    queryKey: ["project-members", projectId],
    queryFn: () => projectApi.listMembers(projectId!),
    enabled: !!projectId,
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
      onClose();
    },
    onError: () => toast.error("Couldn't update the task. Please try again."),
  });

  const deleteMutation = useMutation({
    mutationFn: () => taskApi.remove(task.id),
    onSuccess: () => {
      toast.success("Task deleted");
      void queryClient.invalidateQueries({ queryKey: ["tasks", task.boardListId] });
      onClose();
    },
    onError: () => toast.error("Couldn't delete the task. Please try again."),
  });

  const assignMutation = useMutation({
    mutationFn: (userId: string) => taskApi.assign(task.id, userId),
    onSuccess: () => {
      toast.success("Assignee updated");
      void queryClient.invalidateQueries({ queryKey: ["tasks", task.boardListId] });
    },
    onError: () => toast.error("Could not update assignee."),
  });

  const unassignMutation = useMutation({
    mutationFn: (userId: string) => taskApi.unassign(task.id, userId),
    onSuccess: () => {
      toast.success("Assignee removed");
      void queryClient.invalidateQueries({ queryKey: ["tasks", task.boardListId] });
    },
    onError: () => toast.error("Could not remove assignee."),
  });

  const toggleAssignee = (userId: string) => {
    const isAssigned = task.assignees.some((a) => a.userId === userId);
    if (isAssigned) {
      unassignMutation.mutate(userId);
    } else {
      assignMutation.mutate(userId);
    }
  };

  return (
    <Modal
      isOpen={true}
      onClose={onClose}
      size="xl"
      title={
        <div className="flex items-center gap-2">
          <Sparkles className="h-5 w-5 text-accent-500" />
          <span>Task Details</span>
        </div>
      }
      subtitle="View and edit task information, priority, and assignees"
    >
      <form
        onSubmit={handleSubmit((values) => updateMutation.mutate(values))}
        noValidate
        className="grid grid-cols-1 lg:grid-cols-3 gap-6"
      >
        {/* Main Left Section: Title & Description */}
        <div className="lg:col-span-2 space-y-5">
          {/* Task Title Input */}
          <Input
            label="Title"
            error={errors.title?.message}
            {...register("title")}
            className="text-base font-bold"
            autoFocus
          />

          {/* Description Textarea */}
          <div className="space-y-1.5">
            <label className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-ink-700 dark:text-ink-200">
              <AlignLeft className="h-4 w-4 text-ink-400" />
              <span>Description</span>
            </label>
            <textarea
              className="input-field text-sm leading-relaxed p-3"
              rows={5}
              placeholder="Add a detailed description for this task..."
              {...register("description")}
            />
          </div>

          {/* Assignees List Section */}
          <div className="space-y-2 pt-2">
            <label className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-ink-700 dark:text-ink-200">
              <Users className="h-4 w-4 text-ink-400" />
              <span>Assigned Members</span>
            </label>
            <div className="flex flex-wrap items-center gap-2">
              {task.assignees.map((a) => (
                <div
                  key={a.userId}
                  className="flex items-center gap-1.5 rounded-full border border-ink-200 dark:border-ink-700 bg-ink-50 dark:bg-ink-800 pr-2.5 pl-1 py-1 text-xs"
                >
                  <Avatar name={a.fullName} size="sm" />
                  <span className="font-semibold text-ink-800 dark:text-ink-200">
                    {a.fullName}
                  </span>
                </div>
              ))}

              {/* Assign Member Button */}
              {projectId && (
                <div className="relative">
                  <button
                    type="button"
                    onClick={() => setIsAssigneePopoverOpen((v) => !v)}
                    className="inline-flex items-center gap-1 rounded-full border border-dashed border-ink-300 dark:border-ink-600 px-3 py-1 text-xs font-semibold text-accent-600 hover:bg-accent-50 dark:hover:bg-accent-950/40 transition-colors"
                  >
                    <Plus className="h-3.5 w-3.5" />
                    <span>Assign Member</span>
                  </button>

                  {isAssigneePopoverOpen && (
                    <div className="absolute left-0 top-full mt-2 w-64 rounded-xl border border-ink-100 dark:border-ink-700 bg-white dark:bg-ink-800 p-2 shadow-xl z-50 animate-in fade-in duration-150">
                      <div className="px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-ink-400">
                        Select Project Member
                      </div>
                      <div className="space-y-0.5 max-h-40 overflow-y-auto mt-1">
                        {membersQuery.data?.map((m) => {
                          const isAssigned = task.assignees.some((a) => a.userId === m.userId);
                          return (
                            <button
                              key={m.userId}
                              type="button"
                              onClick={() => toggleAssignee(m.userId)}
                              className="flex w-full items-center justify-between rounded-lg px-2.5 py-1.5 text-left text-xs hover:bg-ink-50 dark:hover:bg-ink-700 transition-colors"
                            >
                              <div className="flex items-center gap-2">
                                <Avatar name={m.fullName} size="sm" />
                                <span className="font-medium text-ink-800 dark:text-ink-200">
                                  {m.fullName}
                                </span>
                              </div>
                              {isAssigned && <Check className="h-4 w-4 text-accent-500" />}
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

        {/* Right Sidebar Metadata Panel */}
        <div className="space-y-5 rounded-2xl border border-ink-100 dark:border-ink-700/80 bg-ink-50/50 dark:bg-ink-900/50 p-4">
          <h4 className="text-xs font-bold uppercase tracking-wider text-ink-500 dark:text-ink-400 border-b border-ink-100 dark:border-ink-700 pb-2">
            Task Configuration
          </h4>

          {/* Priority */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-ink-700 dark:text-ink-200 block">
              Priority
            </label>
            <select className="input-field text-xs py-2" {...register("priority")}>
              {PRIORITIES.map((p) => (
                <option key={p} value={p}>
                  {p.charAt(0) + p.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </div>

          {/* Due Date */}
          <div className="space-y-1.5">
            <Input label="Due Date" type="date" className="text-xs py-2" {...register("dueDate")} />
          </div>

          {/* Tags preview */}
          {task.tags && task.tags.length > 0 && (
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-ink-700 dark:text-ink-200 block">
                Tags
              </label>
              <div className="flex flex-wrap gap-1">
                {task.tags.map((t) => (
                  <span
                    key={t.id}
                    className="rounded-md px-2 py-0.5 text-[11px] font-semibold text-white"
                    style={{ backgroundColor: t.color || "#2F5FF6" }}
                  >
                    {t.name}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="pt-4 border-t border-ink-100 dark:border-ink-700 space-y-2">
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

            <Button type="button" variant="secondary" className="w-full" onClick={onClose}>
              Cancel
            </Button>
          </div>
        </div>
      </form>
    </Modal>
  );
}
