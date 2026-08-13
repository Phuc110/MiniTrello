import { useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  FolderKanban,
  Plus,
  Search,
  Trello,
  ChevronLeft,
  ChevronRight,
  UserPlus,
  MoreVertical,
  Trash2,
} from "lucide-react";
import toast from "react-hot-toast";
import { projectApi } from "@/api/projects";
import { workspaceApi } from "@/api/workspaces";
import { useAuth } from "@/hooks/useAuth";
import { Skeleton } from "@/components/ui/Skeleton";
import { ErrorState } from "@/components/ui/ErrorState";
import { EmptyState } from "@/components/ui/EmptyState";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Badge } from "@/components/ui/Badge";
import { DeleteConfirmModal } from "@/components/ui/DeleteConfirmModal";
import { InviteMemberModal } from "@/components/workspace/InviteMemberModal";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { createProjectSchema, type CreateProjectFormValues } from "@/lib/validation";
import type { Project, Workspace } from "@/types";

export function ProjectListPage() {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [isCreating, setIsCreating] = useState(false);
  const [selectedInviteProject, setSelectedInviteProject] = useState<string | null>(null);
  const [deletingProject, setDeletingProject] = useState<Project | null>(null);
  const [deletingWorkspace, setDeletingWorkspace] = useState<Workspace | null>(null);
  const [isWorkspaceHeaderMenuOpen, setIsWorkspaceHeaderMenuOpen] = useState(false);
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);

  const { data: workspace } = useQuery({
    queryKey: ["workspace", workspaceId],
    queryFn: () => workspaceApi.getById(workspaceId!),
    enabled: !!workspaceId,
  });

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["projects", workspaceId, search, page],
    queryFn: () => projectApi.search(workspaceId!, { name: search || undefined, page, size: 12 }),
    enabled: !!workspaceId,
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateProjectFormValues>({ resolver: zodResolver(createProjectSchema) });

  const createMutation = useMutation({
    mutationFn: (values: CreateProjectFormValues) => projectApi.create(workspaceId!, values),
    onSuccess: () => {
      toast.success("Project created successfully");
      reset();
      setIsCreating(false);
      void queryClient.invalidateQueries({ queryKey: ["projects", workspaceId] });
    },
    onError: () => toast.error("Couldn't create the project. Please try again."),
  });

  const deleteMutation = useMutation({
    mutationFn: (projectId: string) => projectApi.remove(projectId),
    onSuccess: () => {
      toast.success("Project deleted");
      setDeletingProject(null);
      void queryClient.invalidateQueries({ queryKey: ["projects", workspaceId] });
    },
    onError: () => toast.error("Couldn't delete project. Only the owner can delete."),
  });

  const deleteWorkspaceMutation = useMutation({
    mutationFn: (id: string) => workspaceApi.remove(id),
    onSuccess: () => {
      toast.success("Workspace deleted");
      setDeletingWorkspace(null);
      void queryClient.invalidateQueries({ queryKey: ["workspaces"] });
      navigate("/workspaces");
    },
    onError: () => toast.error("Could not delete workspace. Only the owner can delete."),
  });

  return (
    <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-ink-100 dark:border-ink-700 pb-5">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="font-display text-2xl font-bold text-ink-900 dark:text-paper">
              {workspace?.name ?? "Workspace Projects"}
            </h1>
            {/* Workspace ⋯ menu */}
            <div className="relative">
              <button
                id="workspace-header-menu"
                onClick={(e) => { e.stopPropagation(); setIsWorkspaceHeaderMenuOpen((v) => !v); }}
                className="flex h-7 w-7 items-center justify-center rounded-lg text-ink-400 hover:bg-ink-100 dark:hover:bg-ink-700 hover:text-ink-700 dark:hover:text-ink-200 transition-colors"
                aria-label="Workspace options"
              >
                <MoreVertical className="h-4 w-4" />
              </button>
              {isWorkspaceHeaderMenuOpen && workspace && (
                <div className="absolute left-0 top-8 z-20 min-w-[180px] rounded-xl border border-ink-100 dark:border-ink-700 bg-white dark:bg-ink-800 shadow-xl py-1 animate-in fade-in zoom-in-95 duration-150">
                  {workspace.canDelete ?? (workspace.ownerId === user?.id || user?.systemRole === "ADMIN") ? (
                    <button
                      id="delete-workspace-header"
                      onClick={() => { setIsWorkspaceHeaderMenuOpen(false); setDeletingWorkspace(workspace); }}
                      className="flex w-full items-center gap-2.5 px-3 py-2 text-sm font-medium text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors"
                    >
                      <Trash2 className="h-4 w-4 flex-shrink-0" />
                      Delete workspace
                    </button>
                  ) : (
                    <div className="px-3 py-2 text-xs text-ink-400 italic">Owner required to delete</div>
                  )}
                </div>
              )}
            </div>
          </div>
          <p className="text-xs text-ink-400">
            Manage projects, boards, and team members in this workspace
          </p>
        </div>

        <Button onClick={() => setIsCreating(true)}>
          <Plus className="h-4 w-4" />
          <span>New Project</span>
        </Button>
      </div>

      {/* Filter and Search Toolbar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="relative w-full sm:w-80">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" />
          <input
            className="input-field pl-9 py-2 text-xs"
            placeholder="Search projects..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
        </div>
      </div>

      {/* Projects Grid */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {isLoading &&
          Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-44 rounded-xl" />
          ))}

        {isError && (
          <div className="col-span-full">
            <ErrorState message="Could not load projects." onRetry={() => void refetch()} />
          </div>
        )}

        {!isLoading && !isError && data?.content.length === 0 && (
          <div className="col-span-full">
            <EmptyState
              icon={<FolderKanban className="h-10 w-10 text-accent-500" />}
              title={search ? "No matching projects" : "No projects yet"}
              description={
                search
                  ? "No projects match your search query."
                  : "Create your first project to start organizing tasks."
              }
              action={
                !search && (
                  <Button onClick={() => setIsCreating(true)}>
                    <Plus className="h-4 w-4" />
                    <span>Create a Project</span>
                  </Button>
                )
              }
            />
          </div>
        )}

        {data?.content.map((project) => (
          <div
            key={project.id}
            className="group relative flex flex-col justify-between rounded-xl border border-ink-100 dark:border-ink-700 bg-white dark:bg-ink-800 p-5 shadow-xs hover:shadow-md transition-all"
          >
            <div>
              <div className="flex items-center justify-between gap-2">
                <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600 dark:bg-emerald-950/60 dark:text-emerald-400">
                  <FolderKanban className="h-5 w-5" />
                </div>
                <div className="flex items-center gap-1.5">
                  <Badge variant="role" role={project.callerRole}>
                    {project.callerRole}
                  </Badge>

                  {/* ⋯ menu — only shown to OWNERs */}
                  {project.callerRole === "OWNER" && (
                    <div className="relative">
                      <button
                        id={`project-menu-${project.id}`}
                        onClick={(e) => {
                          e.preventDefault();
                          e.stopPropagation();
                          setOpenMenuId(openMenuId === project.id ? null : project.id);
                        }}
                        className="flex h-7 w-7 items-center justify-center rounded-lg text-ink-400 hover:bg-ink-100 dark:hover:bg-ink-700 hover:text-ink-700 dark:hover:text-ink-200 transition-colors"
                        aria-label="Project options"
                      >
                        <MoreVertical className="h-4 w-4" />
                      </button>

                      {openMenuId === project.id && (
                        <div
                          ref={menuRef}
                          className="absolute right-0 top-8 z-20 min-w-[160px] rounded-xl border border-ink-100 dark:border-ink-700 bg-white dark:bg-ink-800 shadow-xl py-1 animate-in fade-in zoom-in-95 duration-150"
                          onClick={(e) => e.stopPropagation()}
                        >
                          <button
                            id={`delete-project-${project.id}`}
                            onClick={() => {
                              setOpenMenuId(null);
                              setDeletingProject(project);
                            }}
                            className="flex w-full items-center gap-2.5 px-3 py-2 text-sm font-medium text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors"
                          >
                            <Trash2 className="h-4 w-4 flex-shrink-0" />
                            Delete project
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>

              <Link
                to={`/projects/${project.id}`}
                className="mt-3 block font-display text-base font-bold text-ink-900 dark:text-paper hover:text-accent-600 dark:hover:text-accent-400 transition-colors"
              >
                {project.name}
              </Link>
              {project.description && (
                <p className="mt-1 text-xs text-ink-400 line-clamp-2">{project.description}</p>
              )}
            </div>

            <div className="mt-5 border-t border-ink-100 dark:border-ink-700/60 pt-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setSelectedInviteProject(project.id)}
                    className="flex items-center gap-1 text-xs font-semibold text-accent-600 hover:text-accent-700 dark:text-accent-400 hover:underline"
                  >
                    <UserPlus className="h-3.5 w-3.5" />
                    <span>Invite</span>
                  </button>
                </div>

                <Link
                  to={`/projects/${project.id}`}
                  className="inline-flex items-center gap-1 rounded-md bg-accent-50 dark:bg-accent-950/60 px-2.5 py-1 text-xs font-semibold text-accent-600 dark:text-accent-300 hover:bg-accent-100 transition-colors"
                >
                  <Trello className="h-3.5 w-3.5" />
                  <span>Open Board</span>
                </Link>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Pagination Controls */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 pt-4">
          <Button
            variant="secondary"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            <ChevronLeft className="h-4 w-4" />
            <span>Previous</span>
          </Button>
          <span className="text-xs font-semibold text-ink-500">
            Page {page + 1} of {data.totalPages}
          </span>
          <Button
            variant="secondary"
            disabled={data.last}
            onClick={() => setPage((p) => p + 1)}
          >
            <span>Next</span>
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      )}

      {/* Create Project Modal */}
      <Modal
        isOpen={isCreating}
        onClose={() => setIsCreating(false)}
        title="Create New Project"
        subtitle="Projects contain task boards, columns, and team members."
      >
        <form
          className="space-y-4"
          onSubmit={handleSubmit((values) => createMutation.mutate(values))}
          noValidate
        >
          <Input
            label="Project Name"
            error={errors.name?.message}
            {...register("name")}
            placeholder="e.g. Website Redesign"
            autoFocus
          />
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-ink-700 dark:text-ink-200">
              Description (optional)
            </label>
            <textarea
              className="input-field py-2 text-xs"
              rows={3}
              {...register("description")}
              placeholder="Brief description of project goals..."
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={() => setIsCreating(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Create Project
            </Button>
          </div>
        </form>
      </Modal>

      {/* Invite Member Modal */}
      {selectedInviteProject && (
        <InviteMemberModal
          projectId={selectedInviteProject}
          isOpen={!!selectedInviteProject}
          onClose={() => setSelectedInviteProject(null)}
        />
      )}

      {/* Click-outside to close menus */}
      {(openMenuId || isWorkspaceHeaderMenuOpen) && (
        <div
          className="fixed inset-0 z-10"
          onClick={() => { setOpenMenuId(null); setIsWorkspaceHeaderMenuOpen(false); }}
          aria-hidden="true"
        />
      )}

      {/* Delete Project Confirmation Modal */}
      {deletingProject && (
        <DeleteConfirmModal
          isOpen={true}
          onClose={() => {
            setDeletingProject(null);
            deleteMutation.reset();
          }}
          onConfirm={() => deleteMutation.mutate(deletingProject.id)}
          isPending={deleteMutation.isPending}
          entityType="project"
          entityName={deletingProject.name}
        />
      )}

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
