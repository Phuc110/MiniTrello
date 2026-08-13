import { Link, useLocation, useParams } from "react-router-dom";
import { ChevronRight, Home, ArrowLeft } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { workspaceApi } from "@/api/workspaces";
import { projectApi } from "@/api/projects";

interface BreadcrumbsProps {
  showBackButton?: boolean;
  backTo?: string;
  backLabel?: string;
}

export function Breadcrumbs({ showBackButton = true, backTo, backLabel }: BreadcrumbsProps) {
  const location = useLocation();
  const { workspaceId, projectId } = useParams<{ workspaceId?: string; projectId?: string }>();

  // Fetch workspace details if workspaceId is present
  const workspaceQuery = useQuery({
    queryKey: ["workspace", workspaceId],
    queryFn: () => workspaceApi.getById(workspaceId!),
    enabled: !!workspaceId,
    retry: false,
  });

  // Fetch project details if projectId is present
  const projectQuery = useQuery({
    queryKey: ["project", projectId],
    queryFn: () => projectApi.getById(projectId!),
    enabled: !!projectId,
    retry: false,
  });

  const activeWorkspaceId = workspaceId || projectQuery.data?.workspaceId;

  const activeWorkspaceQuery = useQuery({
    queryKey: ["workspace", activeWorkspaceId],
    queryFn: () => workspaceApi.getById(activeWorkspaceId!),
    enabled: !!activeWorkspaceId && !workspaceId,
    retry: false,
  });

  const workspace = workspaceQuery.data || activeWorkspaceQuery.data;
  const project = projectQuery.data;

  // Determine back destination
  let computedBackTo = backTo;
  let computedBackLabel = backLabel;

  if (!computedBackTo) {
    if (location.pathname.startsWith("/projects/") && project) {
      computedBackTo = `/workspaces/${project.workspaceId}/projects`;
      computedBackLabel = "Back to Projects";
    } else if (location.pathname.includes("/projects")) {
      computedBackTo = "/workspaces";
      computedBackLabel = "Back to Workspaces";
    }
  }

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-b border-ink-100 dark:border-ink-700 bg-white/70 dark:bg-ink-800/70 backdrop-blur-xs px-4 py-2 text-xs">
      <nav className="flex items-center gap-1.5 text-ink-500 dark:text-ink-400 font-medium">
        <Link
          to="/workspaces"
          className="flex items-center gap-1 hover:text-accent-600 dark:hover:text-accent-400 transition-colors"
        >
          <Home className="h-3.5 w-3.5" aria-hidden="true" />
          <span>Workspaces</span>
        </Link>

        {workspace && (
          <>
            <ChevronRight className="h-3.5 w-3.5 text-ink-300 dark:text-ink-600 flex-shrink-0" />
            <Link
              to={`/workspaces/${workspace.id}/projects`}
              className="hover:text-accent-600 dark:hover:text-accent-400 transition-colors truncate max-w-[150px]"
            >
              {workspace.name}
            </Link>
          </>
        )}

        {project && (
          <>
            <ChevronRight className="h-3.5 w-3.5 text-ink-300 dark:text-ink-600 flex-shrink-0" />
            <span className="text-ink-900 dark:text-paper font-semibold truncate max-w-[180px]">
              {project.name}
            </span>
          </>
        )}
      </nav>

      {showBackButton && computedBackTo && (
        <Link
          to={computedBackTo}
          className="inline-flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-semibold text-ink-700 dark:text-ink-200 bg-ink-50 dark:bg-ink-700 hover:bg-ink-100 dark:hover:bg-ink-600 transition-colors"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          <span>{computedBackLabel || "Back"}</span>
        </Link>
      )}
    </div>
  );
}
