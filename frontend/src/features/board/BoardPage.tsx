import { useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { DragDropContext, Droppable, type DropResult } from "@hello-pangea/dnd";
import {
  Plus,
  Trello,
  Star,
  Search,
  X,
} from "lucide-react";
import toast from "react-hot-toast";
import dayjs from "dayjs";
import { boardApi } from "@/api/boards";
import { taskApi } from "@/api/tasks";
import { KanbanColumn } from "./KanbanColumn";
import { TaskModal } from "@/features/task/TaskModal";
import { BoardFilterPopover, type BoardFilterState } from "./BoardFilterPopover";
import { Skeleton } from "@/components/ui/Skeleton";
import { ErrorState } from "@/components/ui/ErrorState";
import { Button } from "@/components/ui/Button";
import type { Task } from "@/types";

export function BoardPage() {
  const { boardId } = useParams<{ boardId: string }>();
  const queryClient = useQueryClient();
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [isAddingList, setIsAddingList] = useState(false);
  const [newListName, setNewListName] = useState("");
  const [isStarred, setIsStarred] = useState(false);

  const [filterState, setFilterState] = useState<BoardFilterState>({
    search: "",
    priorities: [],
    dueDate: "ALL",
  });

  const boardQuery = useQuery({
    queryKey: ["board", boardId],
    queryFn: () => boardApi.getById(boardId!),
    enabled: !!boardId,
  });

  const listsQuery = useQuery({
    queryKey: ["board-lists", boardId],
    queryFn: () => boardApi.listLists(boardId!),
    enabled: !!boardId,
  });

  const lists = useMemo(
    () => [...(listsQuery.data ?? [])].sort((a, b) => a.position.localeCompare(b.position)),
    [listsQuery.data]
  );

  const taskQueries = useQueries({
    queries: lists.map((list) => ({
      queryKey: ["tasks", list.id],
      queryFn: () => taskApi.listForList(list.id),
      enabled: !!list.id,
    })),
  });

  const createListMutation = useMutation({
    mutationFn: (name: string) => boardApi.createList(boardId!, name),
    onSuccess: () => {
      setNewListName("");
      setIsAddingList(false);
      void queryClient.invalidateQueries({ queryKey: ["board-lists", boardId] });
    },
    onError: () => toast.error("Couldn't create the list. Please try again."),
  });

  const moveTaskMutation = useMutation({
    mutationFn: (variables: {
      taskId: string;
      targetBoardListId: string;
      prevTaskId: string | null;
      nextTaskId: string | null;
    }) =>
      taskApi.move(variables.taskId, {
        targetBoardListId: variables.targetBoardListId,
        prevTaskId: variables.prevTaskId,
        nextTaskId: variables.nextTaskId,
      }),
    onError: () => toast.error("Couldn't move the task. Reverting."),
    onSettled: () => {
      lists.forEach((l) => void queryClient.invalidateQueries({ queryKey: ["tasks", l.id] }));
    },
  });

  const moveListMutation = useMutation({
    mutationFn: ({
      boardListId,
      ...payload
    }: {
      boardListId: string;
      prevListId: string | null;
      nextListId: string | null;
    }) => boardApi.moveList(boardListId, payload),
    onError: () => toast.error("Couldn't reorder the list. Reverting."),
    onSettled: () => void queryClient.invalidateQueries({ queryKey: ["board-lists", boardId] }),
  });

  function handleDragEnd(result: DropResult) {
    const { source, destination, draggableId, type } = result;
    if (!destination) return;
    if (source.droppableId === destination.droppableId && source.index === destination.index) return;

    if (type === "COLUMN") {
      const reordered = [...lists];
      const [moved] = reordered.splice(source.index, 1);
      reordered.splice(destination.index, 0, moved);
      const prevListId = reordered[destination.index - 1]?.id ?? null;
      const nextListId = reordered[destination.index + 1]?.id ?? null;
      moveListMutation.mutate({ boardListId: draggableId, prevListId, nextListId });
      return;
    }

    const destinationListIndex = lists.findIndex((l) => l.id === destination.droppableId);
    const destinationTasks = [...(taskQueries[destinationListIndex]?.data ?? [])];
    const withoutMoved = destinationTasks.filter((t) => t.id !== draggableId);
    withoutMoved.splice(destination.index, 0, { id: draggableId } as Task);

    const prevTaskId = withoutMoved[destination.index - 1]?.id ?? null;
    const nextTaskId = withoutMoved[destination.index + 1]?.id ?? null;

    moveTaskMutation.mutate({
      taskId: draggableId,
      targetBoardListId: destination.droppableId,
      prevTaskId,
      nextTaskId,
    });
  }

  const filterTask = (t: Task) => {
    if (filterState.search) {
      const query = filterState.search.toLowerCase();
      const matchTitle = t.title.toLowerCase().includes(query);
      const matchDesc = t.description?.toLowerCase().includes(query);
      if (!matchTitle && !matchDesc) return false;
    }
    if (filterState.priorities.length > 0) {
      if (!filterState.priorities.includes(t.priority)) return false;
    }
    if (filterState.dueDate !== "ALL") {
      if (filterState.dueDate === "NO_DUE_DATE" && t.dueDate) return false;
      if (filterState.dueDate === "OVERDUE") {
        if (!t.dueDate || !dayjs(t.dueDate).isBefore(dayjs(), "day")) return false;
      }
      if (filterState.dueDate === "DUE_SOON") {
        if (!t.dueDate) return false;
        const diff = dayjs(t.dueDate).diff(dayjs(), "day");
        if (diff < 0 || diff > 7) return false;
      }
    }
    return true;
  };

  if (boardQuery.isLoading || listsQuery.isLoading) {
    return (
      <div className="flex h-full flex-col p-6 space-y-6">
        <Skeleton className="h-10 w-72 rounded-xl" />
        <div className="flex gap-4 overflow-hidden">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-96 w-72 flex-shrink-0 rounded-2xl" />
          ))}
        </div>
      </div>
    );
  }

  if (boardQuery.isError || listsQuery.isError) {
    return (
      <div className="mx-auto max-w-4xl p-10">
        <ErrorState
          message="Could not load this board."
          onRetry={() => {
            void boardQuery.refetch();
            void listsQuery.refetch();
          }}
        />
      </div>
    );
  }

  const board = boardQuery.data;

  return (
    <div className="flex h-full flex-col overflow-hidden bg-ink-100/40 dark:bg-ink-900">
      {/* Board Top Header Bar
          relative z-30: backdrop-blur-md creates a stacking context that
          traps the filter popover at level 0 — without this, kanban cards
          below (later in DOM) paint on top of the popover. */}
      <div className="relative z-30 flex flex-wrap items-center justify-between gap-3 border-b border-ink-200/60 dark:border-ink-700 bg-white/80 dark:bg-ink-800/80 backdrop-blur-md px-4 py-2.5 shadow-2xs">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-accent-500 text-white font-bold shadow-xs">
            <Trello className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-display text-base font-bold text-ink-900 dark:text-paper">
                {board?.name}
              </h1>
              <button
                onClick={() => setIsStarred((v) => !v)}
                className="text-ink-300 hover:text-amber-400 transition-colors"
                title="Star board"
              >
                <Star
                  className={`h-4 w-4 ${isStarred ? "fill-amber-400 text-amber-400" : ""}`}
                />
              </button>
            </div>
          </div>
        </div>

        {/* Action Controls & Filters */}
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-ink-400" />
            <input
              type="text"
              placeholder="Search cards..."
              className="input-field pl-8 py-1.5 text-xs w-36 sm:w-48"
              value={filterState.search}
              onChange={(e) => setFilterState({ ...filterState, search: e.target.value })}
            />
          </div>

          <BoardFilterPopover
            filters={filterState}
            onChange={setFilterState}
            onReset={() =>
              setFilterState({ search: "", priorities: [], dueDate: "ALL" })
            }
          />
        </div>
      </div>

      {/* Kanban Board Drag Drop Canvas
          min-h-0 is critical: a flex child defaults to min-height:auto, so
          without it this row grows with its content instead of staying at the
          viewport height — columns then overflow past the screen and the
          per-column scroll areas never engage. */}
      <div className="min-h-0 flex-1 overflow-x-auto overflow-y-hidden p-4">
        <DragDropContext onDragEnd={handleDragEnd}>
          <Droppable droppableId="board" direction="horizontal" type="COLUMN">
            {(provided) => (
              <div
                ref={provided.innerRef}
                {...provided.droppableProps}
                className="flex h-full items-start gap-4"
              >
                {lists.map((list, index) => {
                  const rawTasks = taskQueries[index]?.data ?? [];
                  const filteredTasks = rawTasks.filter(filterTask);
                  return (
                    <KanbanColumn
                      key={list.id}
                      list={list}
                      index={index}
                      tasks={filteredTasks}
                      isLoading={taskQueries[index]?.isLoading ?? false}
                      onTaskClick={setSelectedTask}
                      boardId={boardId!}
                    />
                  );
                })}
                {provided.placeholder}

                {/* Add Another List Column Form */}
                <div className="w-72 sm:w-80 flex-shrink-0">
                  {isAddingList ? (
                    <form
                      className="rounded-2xl border border-ink-200 dark:border-ink-700 bg-white dark:bg-ink-800 p-3 shadow-md space-y-3"
                      onSubmit={(e) => {
                        e.preventDefault();
                        if (newListName.trim()) createListMutation.mutate(newListName.trim());
                      }}
                    >
                      <input
                        className="input-field text-xs py-2"
                        autoFocus
                        placeholder="Enter list title..."
                        value={newListName}
                        onChange={(e) => setNewListName(e.target.value)}
                      />
                      <div className="flex items-center justify-between gap-2">
                        <Button type="submit" className="py-1 px-3 text-xs">
                          Add list
                        </Button>
                        <button
                          type="button"
                          className="rounded-lg p-1 text-ink-400 hover:bg-ink-100 dark:hover:bg-ink-700"
                          onClick={() => setIsAddingList(false)}
                        >
                          <X className="h-4 w-4" />
                        </button>
                      </div>
                    </form>
                  ) : (
                    <button
                      onClick={() => setIsAddingList(true)}
                      className="flex w-full items-center gap-2 rounded-2xl border-2 border-dashed border-ink-200 dark:border-ink-700/80 bg-white/40 dark:bg-ink-800/40 p-3.5 text-xs font-bold text-ink-600 dark:text-ink-300 hover:border-accent-400 hover:bg-accent-50/50 hover:text-accent-700 transition-all shadow-2xs"
                    >
                      <Plus className="h-4 w-4" />
                      <span>Add another list</span>
                    </button>
                  )}
                </div>
              </div>
            )}
          </Droppable>
        </DragDropContext>
      </div>

      {/* Task Detail Modal */}
      {selectedTask && (
        <TaskModal
          task={selectedTask}
          workspaceId={board?.workspaceId}
          onClose={() => setSelectedTask(null)}
        />
      )}
    </div>
  );
}
