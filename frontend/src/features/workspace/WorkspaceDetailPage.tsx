import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import {
  Plus,
  Trello,
  Trash2,
  LayoutGrid,
} from "lucide-react";
import toast from "react-hot-toast";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import { workspaceApi } from "@/api/workspaces";
import { boardApi } from "@/api/boards";
import { Skeleton } from "@/components/ui/Skeleton";
import { ErrorState } from "@/components/ui/ErrorState";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { DeleteConfirmModal } from "@/components/ui/DeleteConfirmModal";
import type { Board } from "@/types";

dayjs.extend(relativeTime);

export function WorkspaceDetailPage() {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const queryClient = useQueryClient();
  const [isCreating, setIsCreating] = useState(false);
  const [boardName, setBoardName] = useState("");
  const [deletingBoard, setDeletingBoard] = useState<Board | null>(null);

  const workspaceQuery = useQuery({
    queryKey: ["workspace", workspaceId],
    queryFn: () => workspaceApi.getById(workspaceId!),
    enabled: !!workspaceId,
  });

  const boardsQuery = useQuery({
    queryKey: ["boards", workspaceId],
    queryFn: () => boardApi.listByWorkspace(workspaceId!),
    enabled: !!workspaceId,
  });

  const createBoardMutation = useMutation({
    mutationFn: (name: string) => boardApi.create(workspaceId!, name),
    onSuccess: (board) => {
      toast.success(`Board "${board.name}" created`);
      setBoardName("");
      setIsCreating(false);
      void queryClient.invalidateQueries({ queryKey: ["boards", workspaceId] });
    },
    onError: () => toast.error("Could not create board."),
  });

  const deleteBoardMutation = useMutation({
    mutationFn: (id: string) => boardApi.remove(id),
    onSuccess: (_data, deletedId) => {
      toast.success("Board deleted");
      setDeletingBoard(null);
      queryClient.setQueryData<Board[]>(["boards", workspaceId], (old) =>
        (old ?? []).filter((b) => b.id !== deletedId)
      );
    },
    onError: () => toast.error("Could not delete board."),
  });

  const isLoading = workspaceQuery.isLoading || boardsQuery.isLoading;
  const isError = workspaceQuery.isError || boardsQuery.isError;
  const workspace = workspaceQuery.data;
  const boards = boardsQuery.data ?? [];

  if (isLoading) {
    return (
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        <Skeleton className="h-10 w-72 rounded-xl" />
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-44 rounded-2xl" />
          ))}
        </div>
      </div>
    );
  }

  if (isError || !workspace) {
    return (
      <div className="mx-auto max-w-4xl p-10">
        <ErrorState message="Could not load workspace." onRetry={() => void workspaceQuery.refetch()} />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-accent-500 to-indigo-600 shadow-md">
            <Trello className="h-5 w-5 text-white fill-current" />
          </div>
          <div>
            <h1 className="font-display text-2xl font-bold text-ink-900 dark:text-paper">
              {workspace.name}
            </h1>
            <p className="text-xs text-ink-400 font-medium">
              {boards.length} board{boards.length !== 1 ? "s" : ""} · /{workspace.slug}
            </p>
          </div>
        </div>

        <Button onClick={() => setIsCreating(true)}>
          <Plus className="h-4 w-4" />
          <span>New Board</span>
        </Button>
      </div>

      {/* Boards Grid */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {boards.map((board) => (
          <div
            key={board.id}
            className="group relative flex flex-col overflow-visible rounded-2xl border border-ink-100 dark:border-ink-700 bg-white dark:bg-ink-800 shadow-xs hover:-translate-y-0.5 hover:shadow-md transition-all duration-200"
          >
            {/* Delete button */}
            <button
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                setDeletingBoard(board);
              }}
              className="absolute top-2 right-2 z-20 flex h-7 w-7 items-center justify-center rounded-lg bg-black/20 text-white hover:bg-red-500/80 transition-colors opacity-0 group-hover:opacity-100"
              title="Delete board"
            >
              <Trash2 className="h-3.5 w-3.5" />
            </button>

            <Link
              to={`/boards/${board.id}`}
              className="flex flex-col flex-1"
            >
              <div className="h-20 w-full bg-gradient-to-br from-emerald-500 to-teal-600 flex items-end px-5 pb-3 rounded-t-2xl">
                <span className="text-4xl font-display font-extrabold text-white/80 drop-shadow select-none">
                  {board.name.charAt(0).toUpperCase()}
                </span>
              </div>

              <div className="flex flex-col flex-1 px-5 py-4 gap-3">
                <h2 className="font-display text-base font-bold text-ink-900 dark:text-paper group-hover:text-accent-600 dark:group-hover:text-accent-400 transition-colors truncate">
                  {board.name}
                </h2>

                <div className="flex items-center justify-between text-[11px] text-ink-400 border-t border-ink-100 dark:border-ink-700 pt-3">
                  <div className="flex items-center gap-1">
                    <LayoutGrid className="h-3 w-3" />
                    <span>View Board</span>
                  </div>
                  <span>{dayjs(board.createdAt).fromNow()}</span>
                </div>
              </div>
            </Link>
          </div>
        ))}

        {/* Quick-create tile */}
        <button
          onClick={() => setIsCreating(true)}
          className="flex min-h-[176px] flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed border-ink-200 dark:border-ink-700 p-6 text-center text-ink-400 hover:border-accent-400 hover:bg-accent-50/40 dark:hover:bg-accent-950/20 hover:text-accent-600 transition-all duration-200"
        >
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-ink-100 dark:bg-ink-700">
            <Plus className="h-5 w-5" />
          </div>
          <span className="text-sm font-semibold">New Board</span>
        </button>
      </div>

      {/* Create Board Modal */}
      <Modal
        isOpen={isCreating}
        onClose={() => setIsCreating(false)}
        title="Create Board"
        subtitle="Boards contain lists (To Do, In Progress, Done) for organizing tasks."
      >
        <form
          className="space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            if (boardName.trim()) createBoardMutation.mutate(boardName.trim());
          }}
        >
          <Input
            label="Board Name"
            placeholder="e.g. Product Roadmap"
            value={boardName}
            onChange={(e) => setBoardName(e.target.value)}
            autoFocus
          />
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={() => setIsCreating(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={createBoardMutation.isPending}>
              Create Board
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Board Confirmation Modal */}
      {deletingBoard && (
        <DeleteConfirmModal
          isOpen={true}
          onClose={() => {
            setDeletingBoard(null);
            deleteBoardMutation.reset();
          }}
          onConfirm={() => deleteBoardMutation.mutate(deletingBoard.id)}
          isPending={deleteBoardMutation.isPending}
          entityType="board"
          entityName={deletingBoard.name}
        />
      )}
    </div>
  );
}
