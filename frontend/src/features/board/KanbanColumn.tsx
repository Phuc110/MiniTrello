import { useState } from "react";
import { Droppable, Draggable } from "@hello-pangea/dnd";
import { Plus, MoreHorizontal, X, Trash2 } from "lucide-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { boardApi } from "@/api/boards";
import { taskApi } from "@/api/tasks";
import { TaskCard } from "@/features/task/TaskCard";
import { Skeleton } from "@/components/ui/Skeleton";
import type { BoardListColumn, Task } from "@/types";

interface KanbanColumnProps {
  list: BoardListColumn;
  index: number;
  tasks: Task[];
  isLoading: boolean;
  onTaskClick: (task: Task) => void;
  boardId: string;
}

export function KanbanColumn({
  list,
  index,
  tasks,
  isLoading,
  onTaskClick,
  boardId,
}: KanbanColumnProps) {
  const [isAdding, setIsAdding] = useState(false);
  const [title, setTitle] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: (taskTitle: string) => taskApi.create(list.id, { title: taskTitle }),
    onSuccess: () => {
      setTitle("");
      setIsAdding(false);
      void queryClient.invalidateQueries({ queryKey: ["tasks", list.id] });
    },
    onError: () => toast.error("Couldn't create the task. Please try again."),
  });

  const deleteListMutation = useMutation({
    mutationFn: () => boardApi.removeList(list.id),
    onSuccess: () => {
      toast.success(`List "${list.name}" deleted`);
      setMenuOpen(false);
      queryClient.setQueryData<BoardListColumn[]>(
        ["board-lists", boardId],
        (old) => (old ?? []).filter((l) => l.id !== list.id)
      );
      void queryClient.invalidateQueries({ queryKey: ["board-lists", boardId] });
    },
    onError: () => toast.error("Couldn't delete the list. Please try again."),
  });

  return (
    <Draggable draggableId={list.id} index={index}>
      {(columnProvided) => (
        <div
          id={`kanban-column-${list.id}`}
          ref={columnProvided.innerRef}
          {...columnProvided.draggableProps}
          className="flex w-72 sm:w-80 flex-shrink-0 flex-col rounded-2xl border border-ink-200/60 dark:border-ink-700/60 bg-ink-100/60 dark:bg-ink-800/80 p-2.5 max-h-full shadow-xs"
        >
          {/* Column Header — stays pinned (outside the card scroll area) */}
          <div
            {...columnProvided.dragHandleProps}
            className="flex items-center justify-between px-2 py-2 mb-1 border-b border-ink-200/70 dark:border-ink-700/60 cursor-grab active:cursor-grabbing select-none"
          >
            <div className="flex items-center gap-2">
              <h3 className="font-display text-sm font-bold text-ink-900 dark:text-paper">
                {list.name}
              </h3>
              <span className="flex h-5 min-w-[20px] items-center justify-center rounded-full bg-white dark:bg-ink-700 px-1.5 font-mono text-[11px] font-bold text-ink-600 dark:text-ink-300 shadow-2xs">
                {tasks.length}
              </span>
            </div>

            <div className="relative">
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setMenuOpen((v) => !v);
                }}
                className="rounded-lg p-1 text-ink-400 hover:bg-white/60 hover:text-ink-700 dark:hover:bg-ink-700 dark:hover:text-ink-200 transition-colors"
                aria-label="Column options"
              >
                <MoreHorizontal className="h-4 w-4" />
              </button>

              {menuOpen && (
                <>
                  <div className="fixed inset-0 z-30" onClick={() => setMenuOpen(false)} />
                  <div
                    className="absolute right-0 top-8 z-40 min-w-[160px] rounded-xl border border-ink-100 dark:border-ink-700 bg-white dark:bg-ink-800 shadow-xl py-1 animate-in fade-in zoom-in-95 duration-150"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <button
                      onClick={() => {
                        setMenuOpen(false);
                        if (window.confirm(`Delete list "${list.name}" and all its cards?`)) {
                          deleteListMutation.mutate();
                        }
                      }}
                      className="flex w-full items-center gap-2.5 px-3 py-2 text-sm font-medium text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors"
                    >
                      <Trash2 className="h-4 w-4 flex-shrink-0" />
                      Delete list
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>

          {/* Cards Droppable List Container */}
          <Droppable droppableId={list.id} type="TASK">
            {(provided, snapshot) => (
              <div
                ref={provided.innerRef}
                {...provided.droppableProps}
                className={
                  // Cap at ~3 cards (3 × ~110px card + gaps). Cards carry
                  // flex-shrink-0 so flexbox can NOT squeeze them to fit —
                  // the container scrolls instead, with a slim custom scrollbar.
                  "flex min-h-[40px] flex-1 flex-col gap-3 overflow-y-auto rounded-xl p-1.5 max-h-[400px] transition-colors " +
                  "[&::-webkit-scrollbar]:w-1.5 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:bg-gray-300 [&::-webkit-scrollbar-thumb]:rounded-full hover:[&::-webkit-scrollbar-thumb]:bg-gray-400" +
                  (snapshot.isDraggingOver
                    ? " bg-accent-50/50 dark:bg-accent-950/20 ring-2 ring-dashed ring-accent-400/50"
                    : "")
                }
              >
                {isLoading ? (
                  <>
                    <Skeleton className="h-24 rounded-xl flex-shrink-0" />
                    <Skeleton className="h-24 rounded-xl flex-shrink-0" />
                  </>
                ) : (
                  tasks.map((task, taskIndex) => (
                    <TaskCard
                      key={task.id}
                      task={task}
                      index={taskIndex}
                      onClick={() => onTaskClick(task)}
                    />
                  ))
                )}
                {provided.placeholder}
              </div>
            )}
          </Droppable>

          {/* List Footer — stays pinned (outside the card scroll area) */}
          <div className="mt-2 border-t border-ink-200/70 pt-1 dark:border-ink-700/60">
            {isAdding ? (
              <form
                className="flex flex-col gap-2 rounded-xl border border-ink-200 dark:border-ink-700 bg-white dark:bg-ink-800 p-2.5 shadow-sm"
                onSubmit={(e) => {
                  e.preventDefault();
                  if (title.trim()) createMutation.mutate(title.trim());
                }}
              >
                <textarea
                  className="w-full resize-none rounded-lg border-0 bg-transparent p-1 text-sm text-ink-900 dark:text-paper placeholder:text-ink-400 focus:outline-none"
                  rows={2}
                  autoFocus
                  placeholder="Enter a title for this card..."
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && !e.shiftKey) {
                      e.preventDefault();
                      if (title.trim()) createMutation.mutate(title.trim());
                    }
                    if (e.key === "Escape") setIsAdding(false);
                  }}
                />
                <div className="flex items-center justify-between gap-2 pt-1 border-t border-ink-100 dark:border-ink-700">
                  <button
                    type="submit"
                    className="inline-flex items-center justify-center rounded-lg bg-accent-500 px-3 py-1.5 text-xs font-semibold text-white hover:bg-accent-600 transition-colors"
                  >
                    Add Card
                  </button>
                  <button
                    type="button"
                    className="rounded-lg p-1.5 text-ink-400 hover:bg-ink-100 hover:text-ink-700 dark:hover:bg-ink-700 transition-colors"
                    onClick={() => {
                      setIsAdding(false);
                      setTitle("");
                    }}
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
              </form>
            ) : (
              <button
                onClick={() => setIsAdding(true)}
                className="flex w-full items-center gap-1.5 rounded-xl px-3 py-2 text-xs font-semibold text-ink-600 dark:text-ink-300 hover:bg-white/80 dark:hover:bg-ink-700/60 transition-colors"
              >
                <Plus className="h-4 w-4 text-ink-500" aria-hidden="true" />
                <span>Add a card</span>
              </button>
            )}
          </div>
        </div>
      )}
    </Draggable>
  );
}
