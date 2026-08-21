import { Link, useParams } from "react-router-dom";
import { ChevronRight, Home } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { workspaceApi } from "@/api/workspaces";
import { boardApi } from "@/api/boards";

interface BreadcrumbsProps {
  showBackButton?: boolean;
  backTo?: string;
  backLabel?: string;
}

export function Breadcrumbs({ showBackButton = true, backTo, backLabel }: BreadcrumbsProps) {
  const { workspaceId, boardId } = useParams<{ workspaceId?: string; boardId?: string }>();

  const workspaceQuery = useQuery({
    queryKey: ["workspace", workspaceId],
    queryFn: () => workspaceApi.getById(workspaceId!),
    enabled: !!workspaceId,
    retry: false,
  });

  const boardQuery = useQuery({
    queryKey: ["board", boardId],
    queryFn: () => boardApi.getById(boardId!),
    enabled: !!boardId,
    retry: false,
  });

  // If we have a boardId but no workspaceId, fetch the workspace via the board's workspaceId
  const effectiveWorkspaceId = workspaceId || boardQuery.data?.workspaceId;

  const activeWorkspaceQuery = useQuery({
    queryKey: ["workspace", effectiveWorkspaceId],
    queryFn: () => workspaceApi.getById(effectiveWorkspaceId!),
    enabled: !!effectiveWorkspaceId && !workspaceId,
    retry: false,
  });

  const workspace = workspaceQuery.data || activeWorkspaceQuery.data;
  const board = boardQuery.data;

  let computedBackTo = backTo;
  let computedBackLabel = backLabel;

  if (!computedBackTo && workspace) {
    computedBackTo = `/workspaces/${workspace.id}`;
    computedBackLabel = workspace.name;
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
              to={`/workspaces/${workspace.id}`}
              className="hover:text-accent-600 dark:hover:text-accent-400 transition-colors truncate max-w-[150px]"
            >
              {workspace.name}
            </Link>
          </>
        )}

        {board && (
          <>
            <ChevronRight className="h-3.5 w-3.5 text-ink-300 dark:text-ink-600 flex-shrink-0" />
            <span className="text-ink-900 dark:text-paper font-semibold truncate max-w-[180px]">
              {board.name}
            </span>
          </>
        )}
      </nav>

      {showBackButton && computedBackTo && (
        <Link
          to={computedBackTo}
          className="inline-flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-semibold text-ink-700 dark:text-ink-200 bg-ink-50 dark:bg-ink-700 hover:bg-ink-100 dark:hover:bg-ink-600 transition-colors"
        >
          <span>{computedBackLabel || "Back"}</span>
        </Link>
      )}
    </div>
  );
}
