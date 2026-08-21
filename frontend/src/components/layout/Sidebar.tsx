import { useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import {
  Building2,
  LayoutGrid,
  Trello,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { workspaceApi } from "@/api/workspaces";
import { boardApi } from "@/api/boards";
import { MyTasksWidget } from "./MyTasksWidget";
import clsx from "clsx";

interface SidebarProps {
  isMobileOpen?: boolean;
  onCloseMobile?: () => void;
}

export function Sidebar({ isMobileOpen = false, onCloseMobile }: SidebarProps) {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const location = useLocation();
  const { workspaceId } = useParams<{ workspaceId?: string }>();

  const { data: workspaces } = useQuery({
    queryKey: ["workspaces"],
    queryFn: workspaceApi.list,
  });

  const activeWorkspaceId = workspaceId || workspaces?.[0]?.id;

  const { data: boards } = useQuery({
    queryKey: ["boards", activeWorkspaceId],
    queryFn: () => boardApi.listByWorkspace(activeWorkspaceId!),
    enabled: !!activeWorkspaceId,
    retry: false,
  });

  const currentWorkspace = workspaces?.find((w) => w.id === activeWorkspaceId);

  return (
    <>
      {isMobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-ink-900/60 backdrop-blur-xs md:hidden"
          onClick={onCloseMobile}
        />
      )}

      <aside
        className={clsx(
          "fixed top-14 bottom-0 left-0 z-40 flex flex-col border-r border-ink-100 dark:border-ink-700 bg-white dark:bg-ink-800 transition-all duration-300 md:static md:z-0",
          isCollapsed ? "w-16" : "w-64",
          isMobileOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"
        )}
      >
        {/* Workspace Brand / Header */}
        <div className="flex items-center justify-between border-b border-ink-100 dark:border-ink-700 p-3">
          {!isCollapsed && (
            <div className="flex items-center gap-2.5 overflow-hidden">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent-100 text-accent-700 dark:bg-accent-950/60 dark:text-accent-400 font-bold text-sm flex-shrink-0">
                {currentWorkspace?.name?.charAt(0).toUpperCase() || "W"}
              </div>
              <div className="overflow-hidden">
                <p className="text-xs font-bold text-ink-900 dark:text-paper truncate">
                  {currentWorkspace?.name || "Workspace"}
                </p>
                <p className="text-[10px] text-ink-400 font-mono truncate">
                  /{currentWorkspace?.slug || "workspace"}
                </p>
              </div>
            </div>
          )}

          <button
            onClick={() => setIsCollapsed((v) => !v)}
            className="hidden md:flex items-center justify-center rounded-lg p-1.5 text-ink-400 hover:bg-ink-100 dark:hover:bg-ink-700 hover:text-ink-700 transition-colors ml-auto"
            title={isCollapsed ? "Expand sidebar" : "Collapse sidebar"}
          >
            {isCollapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
          </button>
        </div>

        {/* Navigation Items */}
        <div className="flex-1 overflow-y-auto px-2 py-3 space-y-4">
          <nav className="space-y-1">
            <Link
              to="/workspaces"
              onClick={onCloseMobile}
              className={clsx(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-xs font-semibold transition-colors",
                location.pathname === "/workspaces"
                  ? "bg-accent-50 text-accent-700 dark:bg-accent-950/60 dark:text-accent-300"
                  : "text-ink-600 dark:text-ink-300 hover:bg-ink-50 dark:hover:bg-ink-700/60"
              )}
              title="Workspaces"
            >
              <Building2 className="h-4 w-4 text-accent-500 flex-shrink-0" />
              {!isCollapsed && <span>All Workspaces</span>}
            </Link>

            {activeWorkspaceId && (
              <Link
                to={`/workspaces/${activeWorkspaceId}`}
                onClick={onCloseMobile}
                className={clsx(
                  "flex items-center gap-3 rounded-lg px-3 py-2 text-xs font-semibold transition-colors",
                  location.pathname === `/workspaces/${activeWorkspaceId}` && !location.pathname.includes("/boards/")
                    ? "bg-accent-50 text-accent-700 dark:bg-accent-950/60 dark:text-accent-300"
                    : "text-ink-600 dark:text-ink-300 hover:bg-ink-50 dark:hover:bg-ink-700/60"
                )}
                title="Boards"
              >
                <LayoutGrid className="h-4 w-4 text-emerald-500 flex-shrink-0" />
                {!isCollapsed && <span>Boards</span>}
              </Link>
            )}
          </nav>

          {/* Boards Quick List */}
          {!isCollapsed && boards && boards.length > 0 && (
            <div className="pt-2 border-t border-ink-100 dark:border-ink-700">
              <div className="flex items-center justify-between px-3 py-1 mb-1">
                <span className="text-[10px] font-bold uppercase tracking-wider text-ink-400">
                  Boards ({boards.length})
                </span>
              </div>
              <div className="space-y-0.5 max-h-48 overflow-y-auto">
                {boards.map((b) => {
                  const isActive = location.pathname.includes(`/boards/${b.id}`);
                  return (
                    <Link
                      key={b.id}
                      to={`/boards/${b.id}`}
                      onClick={onCloseMobile}
                      className={clsx(
                        "flex items-center gap-2.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors truncate",
                        isActive
                          ? "bg-accent-50 text-accent-700 dark:bg-accent-950/60 dark:text-accent-300 font-semibold"
                          : "text-ink-600 dark:text-ink-300 hover:bg-ink-50 dark:hover:bg-ink-700/60"
                      )}
                    >
                      <Trello className="h-3.5 w-3.5 text-ink-400 flex-shrink-0" />
                      <span className="truncate">{b.name}</span>
                    </Link>
                  );
                })}
              </div>
            </div>
          )}

          {/* My Assigned Tasks */}
          {!isCollapsed && <MyTasksWidget />}
        </div>
      </aside>
    </>
  );
}
