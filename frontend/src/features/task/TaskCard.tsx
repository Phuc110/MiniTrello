import { Draggable } from "@hello-pangea/dnd";
import dayjs from "dayjs";
import { CalendarDays, MoreHorizontal } from "lucide-react";
import clsx from "clsx";
import { Avatar } from "@/components/ui/Avatar";
import { Badge } from "@/components/ui/Badge";
import type { Task } from "@/types";

interface TaskCardProps {
  task: Task;
  index: number;
  onClick: () => void;
}

const PRIORITY_COLOR: Record<Task["priority"], string> = {
  LOW: "bg-ink-400",
  MEDIUM: "bg-blue-500",
  HIGH: "bg-amber-500",
  URGENT: "bg-red-500",
};

export function TaskCard({ task, index, onClick }: TaskCardProps) {
  const isOverdue = task.dueDate && dayjs(task.dueDate).isBefore(dayjs(), "day");

  return (
    <Draggable draggableId={task.id} index={index}>
      {(provided, snapshot) => (
        <div
          ref={provided.innerRef}
          {...provided.draggableProps}
          {...provided.dragHandleProps}
          onClick={onClick}
          className={clsx(
            "group relative w-full overflow-hidden rounded-xl border border-ink-100 dark:border-ink-700/80",
            "bg-white dark:bg-ink-800 p-3.5 text-left shadow-xs transition-all duration-150 cursor-grab active:cursor-grabbing hover:shadow-md hover:border-ink-200 dark:hover:border-ink-600",
            snapshot.isDragging && "shadow-xl ring-2 ring-accent-500 rotate-1 scale-[1.02] z-50 opacity-95"
          )}
        >
          {/* Priority Left Spine Accent */}
          <span
            className={clsx("absolute left-0 top-0 h-full w-1 rounded-l-xl", PRIORITY_COLOR[task.priority])}
            aria-hidden="true"
          />

          {/* Card Header: Priority & Quick Actions */}
          <div className="flex items-center justify-between gap-2 mb-1.5 pl-1">
            <Badge variant="priority" priority={task.priority}>
              {task.priority}
            </Badge>

            <button
              onClick={(e) => {
                e.stopPropagation();
                onClick();
              }}
              className="opacity-0 group-hover:opacity-100 rounded-md p-1 text-ink-400 hover:bg-ink-100 hover:text-ink-700 dark:hover:bg-ink-700 transition-all"
              title="Card options"
            >
              <MoreHorizontal className="h-3.5 w-3.5" />
            </button>
          </div>

          {/* Task Title */}
          <h4 className="pl-1 text-sm font-semibold text-ink-900 dark:text-paper leading-snug">
            {task.title}
          </h4>

          {/* Short Description Snippet if present */}
          {task.description && (
            <p className="pl-1 mt-1 text-xs text-ink-400 line-clamp-2 leading-relaxed">
              {task.description}
            </p>
          )}

          {/* Tag Badges */}
          {task.tags && task.tags.length > 0 && (
            <div className="pl-1 mt-2.5 flex flex-wrap items-center gap-1.5">
              {task.tags.map((tag) => (
                <span
                  key={tag.id}
                  className="rounded-md px-2 py-0.5 text-[10px] font-semibold text-white shadow-xs"
                  style={{ backgroundColor: tag.color || "#2F5FF6" }}
                >
                  {tag.name}
                </span>
              ))}
            </div>
          )}

          {/* Footer Metadata: Due Date & Assignee Avatars */}
          <div className="pl-1 mt-3 flex items-center justify-between border-t border-ink-100 dark:border-ink-700/60 pt-2.5">
            {task.dueDate ? (
              <span
                className={clsx(
                  "flex items-center gap-1 font-mono text-[11px] font-semibold rounded-md px-1.5 py-0.5",
                  isOverdue
                    ? "bg-red-50 text-red-600 dark:bg-red-950/50 dark:text-red-300"
                    : "bg-ink-50 text-ink-500 dark:bg-ink-700/60 dark:text-ink-300"
                )}
              >
                <CalendarDays className="h-3 w-3" />
                {dayjs(task.dueDate).format("MMM D")}
              </span>
            ) : (
              <span />
            )}

            {/* Assignees Avatar Pile */}
            {task.assignees && task.assignees.length > 0 && (
              <div className="flex -space-x-1.5 ml-auto">
                {task.assignees.slice(0, 3).map((assignee) => (
                  <Avatar
                    key={assignee.userId}
                    name={assignee.fullName}
                    size="sm"
                    title={assignee.fullName}
                  />
                ))}
                {task.assignees.length > 3 && (
                  <span className="flex h-6 w-6 items-center justify-center rounded-full bg-ink-200 text-[9px] font-bold text-ink-700 border-2 border-white dark:border-ink-800">
                    +{task.assignees.length - 3}
                  </span>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </Draggable>
  );
}
