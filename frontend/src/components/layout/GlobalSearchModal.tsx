import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Search, Building2, FolderKanban, X } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { workspaceApi } from "@/api/workspaces";
import { projectApi } from "@/api/projects";
import { Modal } from "@/components/ui/Modal";

interface GlobalSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function GlobalSearchModal({ isOpen, onClose }: GlobalSearchModalProps) {
  const [term, setTerm] = useState("");
  const navigate = useNavigate();

  const workspacesQuery = useQuery({
    queryKey: ["workspaces"],
    queryFn: workspaceApi.list,
    enabled: isOpen,
  });

  const firstWorkspace = workspacesQuery.data?.[0];

  const projectsQuery = useQuery({
    queryKey: ["projects", firstWorkspace?.id, term],
    queryFn: () => projectApi.search(firstWorkspace!.id, { name: term || undefined, size: 20 }),
    enabled: isOpen && !!firstWorkspace && term.trim().length > 0,
  });

  const matchingWorkspaces = (workspacesQuery.data ?? []).filter((w) =>
    w.name.toLowerCase().includes(term.toLowerCase())
  );

  const matchingProjects = projectsQuery.data?.content ?? [];

  useEffect(() => {
    if (!isOpen) {
      setTerm("");
    }
  }, [isOpen]);

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="lg">
      <div className="flex flex-col gap-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-ink-400" />
          <input
            autoFocus
            type="text"
            className="w-full rounded-xl border border-ink-200 dark:border-ink-600 bg-ink-50/50 dark:bg-ink-900/50 pl-11 pr-10 py-3 text-base text-ink-900 dark:text-paper placeholder:text-ink-400 focus:outline-none focus:ring-2 focus:ring-accent-500"
            placeholder="Search workspaces, projects..."
            value={term}
            onChange={(e) => setTerm(e.target.value)}
          />
          {term && (
            <button
              onClick={() => setTerm("")}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-ink-400 hover:text-ink-600"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>

        <div className="flex flex-col gap-4 max-h-[60vh] overflow-y-auto pr-1">
          {!term.trim() && (
            <div className="py-8 text-center text-sm text-ink-400">
              Type something to search across workspaces and projects...
            </div>
          )}

          {term.trim() && (
            <>
              {matchingWorkspaces.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-ink-400 mb-2 px-1">
                    Workspaces ({matchingWorkspaces.length})
                  </h4>
                  <div className="flex flex-col gap-1">
                    {matchingWorkspaces.map((w) => (
                      <button
                        key={w.id}
                        onClick={() => {
                          navigate(`/workspaces/${w.id}/projects`);
                          onClose();
                        }}
                        className="flex items-center gap-3 rounded-lg p-2.5 text-left hover:bg-ink-50 dark:hover:bg-ink-700/60 transition-colors"
                      >
                        <Building2 className="h-4 w-4 text-accent-500 flex-shrink-0" />
                        <div>
                          <p className="text-sm font-semibold text-ink-900 dark:text-paper">
                            {w.name}
                          </p>
                          <p className="text-xs text-ink-400 font-mono">/{w.slug}</p>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {matchingProjects.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-ink-400 mb-2 px-1">
                    Projects ({matchingProjects.length})
                  </h4>
                  <div className="flex flex-col gap-1">
                    {matchingProjects.map((p) => (
                      <button
                        key={p.id}
                        onClick={() => {
                          navigate(`/projects/${p.id}`);
                          onClose();
                        }}
                        className="flex items-center gap-3 rounded-lg p-2.5 text-left hover:bg-ink-50 dark:hover:bg-ink-700/60 transition-colors"
                      >
                        <FolderKanban className="h-4 w-4 text-emerald-500 flex-shrink-0" />
                        <div>
                          <p className="text-sm font-semibold text-ink-900 dark:text-paper">
                            {p.name}
                          </p>
                          {p.description && (
                            <p className="text-xs text-ink-400 line-clamp-1">{p.description}</p>
                          )}
                        </div>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {matchingWorkspaces.length === 0 && matchingProjects.length === 0 && (
                <div className="py-8 text-center text-sm text-ink-400">
                  No matching workspaces or projects found for &quot;{term}&quot;.
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </Modal>
  );
}
