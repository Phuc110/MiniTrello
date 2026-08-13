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
  UserPlus,
} from "lucide-react";
import toast from "react-hot-toast";
import dayjs from "dayjs";
import { projectApi } from "@/api/projects";
import { boardApi } from "@/api/boards";
import { taskApi } from "@/api/tasks";
import { KanbanColumn } from "./KanbanColumn";
import { TaskModal } from "@/features/task/TaskModal";
import { BoardFilterPopover, type BoardFilterState } from "./BoardFilterPopover";
import { InviteMemberModal } from "@/components/workspace/InviteMemberModal";
import { Skeleton } from "@/components/ui/Skeleton";
import { ErrorState } from "@/components/ui/ErrorState";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import type { Task } from "@/types";

export function BoardPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const queryClient = useQueryClient();
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [isAddingList, setIsAddingList] = useState(false);
  const [newListName, setNewListName] = useState("");
  const [isStarred, setIsStarred] = useState(false);
  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false);

  // Filter state
  const [filterState, setFilterState] = useState<BoardFilterState>({
    search: "",
    priorities: [],
    dueDate: "ALL",
  });

  const projectQuery = useQuery({
    queryKey: ["project", projectId],
    queryFn: () => projectApi.getById(projectId!),
    enabled: !!projectId,
  });

  const membersQuery = useQuery({
    queryKey: ["project-members", projectId],
    queryFn: () => projectApi.listMembers(projectId!),
    enabled: !!projectId,
  });

  const boardsQuery = useQuery({
    queryKey: ["boards", projectId],
    queryFn: () => boardApi.list(projectId!),
    enabled: !!projectId,
  });

  const createBoardMutation = useMutation({
    mutationFn: () => boardApi.create(projectId!, "Main Board"),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["boards", projectId] }),
  });

  const board = boardsQuery.data?.[0];

  const listsQuery = useQuery({
    queryKey: ["board-lists", board?.id],
    queryFn: () => boardApi.listLists(board!.id),
    enabled: !!board,
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
    mutationFn: (name: string) => boardApi.createList(board!.id, name),
    onSuccess: () => {
      setNewListName("");
      setIsAddingList(false);
      void queryClient.invalidateQueries({ queryKey: ["board-lists", board?.id] });
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
    onSettled: () => void queryClient.invalidateQueries({ queryKey: ["board-lists", board?.id] }),
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

  // Filter tasks client-side based on filterState
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

  if (projectQuery.isLoading || boardsQuery.isLoading) {
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

  if (projectQuery.isError || boardsQuery.isError) {
    return (
      <div className="mx-auto max-w-4xl p-10">
        <ErrorState
          message="Could not load this board."
          onRetry={() => {
            void projectQuery.refetch();
            void boardsQuery.refetch();
          }}
        />
      </div>
    );
  }

  if (!board) {
    return (
      <div className="mx-auto max-w-2xl p-12 text-center space-y-4">
        <Trello className="h-12 w-12 text-accent-500 mx-auto" />
        <h2 className="font-display text-xl font-bold">No Board Created Yet</h2>
        <p className="text-sm text-ink-400">
          This project doesn&apos;t have a Kanban board initialized yet.
        </p>
        <Button
          onClick={() => createBoardMutation.mutate()}
          isLoading={createBoardMutation.isPending}
        >
          <Plus className="h-4 w-4" />
          <span>Create Main Board</span>
        </Button>
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col overflow-hidden bg-ink-100/40 dark:bg-ink-900">
      {/* Board Top Header Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-ink-200/60 dark:border-ink-700 bg-white/80 dark:bg-ink-800/80 backdrop-blur-md px-4 py-2.5 shadow-2xs">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-accent-500 text-white font-bold shadow-xs">
            <Trello className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-display text-base font-bold text-ink-900 dark:text-paper">
                {projectQuery.data?.name} — {board.name}
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
            {projectQuery.data?.description && (
              <p className="text-xs text-ink-400 line-clamp-1">
                {projectQuery.data.description}
              </p>
            )}
          </div>
        </div>

        {/* Action Controls & Filters */}
        <div className="flex items-center gap-2">
          {/* Search Cards Input */}
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

          {/* Filter Popover */}
          <BoardFilterPopover
            filters={filterState}
            onChange={setFilterState}
            onReset={() =>
              setFilterState({ search: "", priorities: [], dueDate: "ALL" })
            }
          />

          {/* Members Pile & Invite Button */}
          <div className="flex items-center gap-1.5 pl-2 border-l border-ink-200 dark:border-ink-700">
            <div className="flex -space-x-1.5">
              {membersQuery.data?.slice(0, 4).map((m) => (
                <Avatar key={m.userId} name={m.fullName} size="sm" />
              ))}
            </div>
            <Button
              variant="secondary"
              className="py-1 px-2.5 text-xs"
              onClick={() => setIsInviteModalOpen(true)}
            >
              <UserPlus className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">Invite</span>
            </Button>
          </div>
        </div>
      </div>

      {/* Kanban Board Drag Drop Canvas */}
      <div className="flex-1 overflow-x-auto p-4">
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
          projectId={projectId}
          onClose={() => setSelectedTask(null)}
        />
      )}

      {/* Invite Member Modal */}
      {projectId && (
        <InviteMemberModal
          projectId={projectId}
          isOpen={isInviteModalOpen}
          onClose={() => setIsInviteModalOpen(false)}
        />
      )}
    </div>
  );
}
