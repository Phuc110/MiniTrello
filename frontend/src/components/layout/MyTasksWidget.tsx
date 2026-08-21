import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ListChecks, ChevronDown, ChevronRight, Clock, AlertTriangle } from "lucide-react";
import dayjs from "dayjs";
import { taskApi } from "@/api/tasks";
import { TaskModal } from "@/features/task/TaskModal";
import type { Task } from "@/types";

const PRIORITY_STYLES: Record<string, string> = {
  URGENT: "bg-red-500",
  HIGH: "bg-amber-500",
  MEDIUM: "bg-blue-500",
  LOW: "bg-ink-300 dark:bg-ink-600",
};

function getDueDateStatus(task: Task): { label: string; color: string; icon: React.ReactNode } | null {
  if (!task.dueDate) return null;
  const now = dayjs();
  const due = dayjs(task.dueDate);
  const diff = due.diff(now, "day");

  if (diff < 0) return { label: "Overdue", color: "text-red-600 dark:text-red-400", icon: <AlertTriangle className="h-3 w-3" /> };
  if (diff === 0) return { label: "Due today", color: "text-amber-600 dark:text-amber-400", icon: <Clock className="h-3 w-3" /> };
  if (diff <= 2) return { label: `Due in ${diff}d`, color: "text-amber-500 dark:text-amber-400", icon: <Clock className="h-3 w-3" /> };
  return null;
}

export function MyTasksWidget() {
  const [isExpanded, setIsExpanded] = useState(true);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);

  const { data: tasks, isLoading } = useQuery({
    queryKey: ["my-tasks"],
    queryFn: taskApi.myTasks,
    refetchInterval: 60_000,
  });

  const taskList = tasks ?? [];
  const overdueCount = taskList.filter((t) => {
    if (!t.dueDate) return false;
    return dayjs(t.dueDate).isBefore(dayjs(), "day");
  }).length;

  if (isLoading) {
    return (
      <div className="px-3 py-2">
        <div className="h-4 w-24 rounded bg-ink-100 dark:bg-ink-700 animate-pulse" />
      </div>
    );
  }

  if (taskList.length === 0) return null;

  return (
    <>
      <div className="pt-2 border-t border-ink-100 dark:border-ink-700">
        <button
          onClick={() => setIsExpanded((v) => !v)}
          className="flex w-full items-center justify-between px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-ink-400 hover:text-ink-600 dark:hover:text-ink-300 transition-colors"
        >
          <div className="flex items-center gap-1.5">
            <ListChecks className="h-3.5 w-3.5 text-accent-500" />
            <span>My Tasks ({taskList.length})</span>
            {overdueCount > 0 && (
              <span className="rounded-full bg-red-500 text-white text-[9px] font-bold px-1.5 py-0.5 leading-none">
                {overdueCount}
              </span>
            )}
          </div>
          {isExpanded ? <ChevronDown className="h-3 w-3" /> : <ChevronRight className="h-3 w-3" />}
        </button>

        {isExpanded && (
          <div className="space-y-0.5 max-h-48 overflow-y-auto px-1 mt-1">
            {taskList.slice(0, 10).map((task) => {
              const dueStatus = getDueDateStatus(task);
              return (
                <button
                  key={task.id}
                  onClick={() => setSelectedTask(task)}
                  className="flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-left text-xs hover:bg-ink-50 dark:hover:bg-ink-700/60 transition-colors group"
                >
                  <span className={`h-1.5 w-1.5 rounded-full flex-shrink-0 ${PRIORITY_STYLES[task.priority] || "bg-ink-300"}`} />
                  <span className="truncate text-ink-800 dark:text-ink-200 group-hover:text-accent-600 dark:group-hover:text-accent-400 font-medium">
                    {task.title}
                  </span>
                  {dueStatus && (
                    <span className={`flex items-center gap-0.5 text-[9px] font-semibold flex-shrink-0 ${dueStatus.color}`}>
                      {dueStatus.icon}
                      {dueStatus.label}
                    </span>
                  )}
                </button>
              );
            })}
            {taskList.length > 10 && (
              <p className="text-[10px] text-ink-400 text-center py-1">
                +{taskList.length - 10} more tasks
              </p>
            )}
          </div>
        )}
      </div>

      {selectedTask && (
        <TaskModal
          task={selectedTask}
          onClose={() => setSelectedTask(null)}
        />
      )}
    </>
  );
}
