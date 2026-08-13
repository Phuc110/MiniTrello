import { useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router-dom";
import {
  Building2,
  Plus,
  Search,
  Trello,
  FolderKanban,
  Clock,
  ChevronRight,
  MoreVertical,
  Trash2,
} from "lucide-react";
import toast from "react-hot-toast";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import { workspaceApi } from "@/api/workspaces";
import { useAuth } from "@/hooks/useAuth";
import { Skeleton } from "@/components/ui/Skeleton";
import { ErrorState } from "@/components/ui/ErrorState";
import { EmptyState } from "@/components/ui/EmptyState";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { DeleteConfirmModal } from "@/components/ui/DeleteConfirmModal";
import type { Workspace } from "@/types";

dayjs.extend(relativeTime);

export function WorkspaceListPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [isCreatingWorkspace, setIsCreatingWorkspace] = useState(false);
  const [workspaceName, setWorkspaceName] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [deletingWorkspace, setDeletingWorkspace] = useState<Workspace | null>(null);
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);

  const { data: workspaces, isLoading, isError, refetch } = useQuery({
    queryKey: ["workspaces"],
    queryFn: workspaceApi.list,
  });

  const createWorkspaceMutation = useMutation({
    mutationFn: workspaceApi.create,
    onSuccess: (newWorkspace) => {
      toast.success(`Workspace "${newWorkspace.name}" created`);
      setWorkspaceName("");
      setIsCreatingWorkspace(false);
      void queryClient.invalidateQueries({ queryKey: ["workspaces"] });
      navigate(`/workspaces/${newWorkspace.id}/projects`);
    },
    onError: () => toast.error("Could not create workspace."),
  });

  const deleteWorkspaceMutation = useMutation({
    mutationFn: (id: string) => workspaceApi.remove(id),
    onSuccess: () => {
      toast.success("Workspace deleted");
      setDeletingWorkspace(null);
      void queryClient.invalidateQueries({ queryKey: ["workspaces"] });
      navigate("/workspaces");
    },
    onError: () => toast.error("Could not delete workspace. Only the owner can delete it."),
  });

  const filtered = (workspaces ?? []).filter((w) =>
    w.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const GRADIENTS = [
    "from-accent-500 to-indigo-600",
    "from-violet-500 to-purple-700",
    "from-emerald-500 to-teal-700",
    "from-rose-500 to-pink-700",
    "from-amber-500 to-orange-600",
    "from-sky-500 to-blue-700",
  ];
  function gradientFor(name: string) {
    let hash = 0;
    for (let i = 0; i < name.length; i++) hash = (hash * 31 + name.charCodeAt(i)) >>> 0;
    return GRADIENTS[hash % GRADIENTS.length];
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
              My Workspaces
            </h1>
            <p className="text-xs text-ink-400 font-medium">
              {workspaces?.length ?? 0} workspace{(workspaces?.length ?? 0) !== 1 ? "s" : ""}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3 w-full sm:w-auto">
          <div className="relative flex-1 sm:w-64">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" />
            <input
              type="text"
              placeholder="Search workspaces..."
              className="input-field pl-9 py-1.5 text-xs"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <Button onClick={() => setIsCreatingWorkspace(true)}>
            <Plus className="h-4 w-4" />
            <span>New Workspace</span>
          </Button>
        </div>
      </div>

      {/* Workspaces Grid */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">

        {isLoading &&
          Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-44 rounded-2xl" />
          ))}

        {isError && (
          <div className="col-span-full">
            <ErrorState message="Could not load workspaces." onRetry={() => void refetch()} />
          </div>
        )}

        {!isLoading && !isError && (workspaces ?? []).length === 0 && (
          <div className="col-span-full">
            <EmptyState
              icon={<Building2 className="h-10 w-10 text-accent-500" />}
              title="No Workspaces Yet"
              description="Create your first workspace to start organizing projects and boards."
              action={
                <Button onClick={() => setIsCreatingWorkspace(true)}>
                  <Plus className="h-4 w-4" />
                  <span>Create Workspace</span>
                </Button>
              }
            />
          </div>
        )}

        {!isLoading && !isError && (workspaces ?? []).length > 0 && filtered.length === 0 && (
          <div className="col-span-full py-12 text-center text-ink-400 text-sm">
            No workspaces match &ldquo;{searchTerm}&rdquo;
          </div>
        )}

        {/* One card per workspace */}
        {filtered.map((workspace) => {
          const isOwner = workspace.canDelete ?? (workspace.ownerId === user?.id || user?.systemRole === "ADMIN");

          return (
            <div
              key={workspace.id}
              className="group relative flex flex-col overflow-visible rounded-2xl border border-ink-100 dark:border-ink-700 bg-white dark:bg-ink-800 shadow-xs hover:-translate-y-0.5 hover:shadow-md transition-all duration-200"
            >
              {/* ⋯ menu button */}
              <div className="absolute top-2 right-2 z-10">
                <button
                  id={`workspace-menu-${workspace.id}`}
                  onClick={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    setOpenMenuId(openMenuId === workspace.id ? null : workspace.id);
                  }}
                  className="flex h-7 w-7 items-center justify-center rounded-lg bg-black/20 text-white hover:bg-black/40 transition-colors"
                  aria-label="Workspace options"
                >
                  <MoreVertical className="h-4 w-4" />
                </button>

                {openMenuId === workspace.id && (
                  <div
                    ref={menuRef}
                    className="absolute right-0 top-8 z-20 min-w-[160px] rounded-xl border border-ink-100 dark:border-ink-700 bg-white dark:bg-ink-800 shadow-xl py-1 animate-in fade-in zoom-in-95 duration-150"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {isOwner ? (
                      <button
                        id={`delete-workspace-${workspace.id}`}
                        onClick={() => {
                          setOpenMenuId(null);
                          setDeletingWorkspace(workspace);
                        }}
                        className="flex w-full items-center gap-2.5 px-3 py-2 text-sm font-medium text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors"
                      >
                        <Trash2 className="h-4 w-4 flex-shrink-0" />
                        Delete workspace
                      </button>
                    ) : (
                      <div className="px-3 py-2 text-xs font-medium text-ink-400 italic">
                        Owner permissions required to delete
                      </div>
                    )}
                  </div>
                )}
              </div>

            {/* Card content — navigates to projects */}
            <Link
              to={`/workspaces/${workspace.id}/projects`}
              className="flex flex-col flex-1"
              onClick={() => setOpenMenuId(null)}
            >
              <div
                className={`h-20 w-full bg-gradient-to-br ${gradientFor(workspace.name)} flex items-end px-5 pb-3 rounded-t-2xl`}
              >
                <span className="text-4xl font-display font-extrabold text-white/80 drop-shadow select-none">
                  {workspace.name.charAt(0).toUpperCase()}
                </span>
              </div>

              <div className="flex flex-col flex-1 px-5 py-4 gap-3">
                <div>
                  <h2 className="font-display text-base font-bold text-ink-900 dark:text-paper group-hover:text-accent-600 dark:group-hover:text-accent-400 transition-colors truncate">
                    {workspace.name}
                  </h2>
                  <p className="text-[11px] text-ink-400 font-mono mt-0.5 truncate">
                    /{workspace.slug}
                  </p>
                </div>

                <div className="flex items-center justify-between text-[11px] text-ink-400 border-t border-ink-100 dark:border-ink-700 pt-3">
                  <div className="flex items-center gap-1">
                    <Clock className="h-3 w-3" />
                    <span>{dayjs(workspace.createdAt).fromNow()}</span>
                  </div>
                  <div className="flex items-center gap-1 font-semibold text-accent-600 dark:text-accent-400">
                    <FolderKanban className="h-3.5 w-3.5" />
                    <span>View Projects</span>
                    <ChevronRight className="h-3 w-3 group-hover:translate-x-0.5 transition-transform" />
                  </div>
                </div>
              </div>
            </Link>
          </div>
        );
      })}

        {/* Quick-create tile */}
        {!isLoading && !isError && (
          <button
            onClick={() => setIsCreatingWorkspace(true)}
            className="flex min-h-[176px] flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed border-ink-200 dark:border-ink-700 p-6 text-center text-ink-400 hover:border-accent-400 hover:bg-accent-50/40 dark:hover:bg-accent-950/20 hover:text-accent-600 transition-all duration-200"
          >
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-ink-100 dark:bg-ink-700">
              <Plus className="h-5 w-5" />
            </div>
            <span className="text-sm font-semibold">New Workspace</span>
          </button>
        )}
      </div>

      {/* Click-outside to close menu */}
      {openMenuId && (
        <div
          className="fixed inset-0 z-10"
          onClick={() => setOpenMenuId(null)}
          aria-hidden="true"
        />
      )}

      {/* Create Workspace Modal */}
      <Modal
        isOpen={isCreatingWorkspace}
        onClose={() => setIsCreatingWorkspace(false)}
        title="Create Workspace"
        subtitle="Workspaces group projects and team members together."
      >
        <form
          className="space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            if (workspaceName.trim()) {
              createWorkspaceMutation.mutate(workspaceName.trim());
            }
          }}
        >
          <Input
            label="Workspace Name"
            placeholder="e.g. Engineering Team"
            value={workspaceName}
            onChange={(e) => setWorkspaceName(e.target.value)}
            autoFocus
          />
          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsCreatingWorkspace(false)}
            >
              Cancel
            </Button>
            <Button type="submit" isLoading={createWorkspaceMutation.isPending}>
              Create Workspace
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Workspace Confirmation Modal */}
      {deletingWorkspace && (
        <DeleteConfirmModal
          isOpen={true}
          onClose={() => {
            setDeletingWorkspace(null);
            deleteWorkspaceMutation.reset();
          }}
          onConfirm={() => deleteWorkspaceMutation.mutate(deletingWorkspace.id)}
          isPending={deleteWorkspaceMutation.isPending}
          entityType="workspace"
          entityName={deletingWorkspace.name}
        />
      )}
    </div>
  );
}
