import { useState, useRef, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Trello,
  Search,
  ChevronDown,
  LogOut,
  Menu,
  Building2,
  Plus,
} from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/hooks/useAuth";
import { workspaceApi } from "@/api/workspaces";
import { Avatar } from "@/components/ui/Avatar";
import { GlobalSearchModal } from "./GlobalSearchModal";
import { NotificationBell } from "./NotificationBell";

interface AppHeaderProps {
  onToggleSidebar?: () => void;
}

export function AppHeader({ onToggleSidebar }: AppHeaderProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [isWorkspaceMenuOpen, setIsWorkspaceMenuOpen] = useState(false);
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [isSearchOpen, setIsSearchOpen] = useState(false);

  const workspaceRef = useRef<HTMLDivElement>(null);
  const userRef = useRef<HTMLDivElement>(null);

  const { data: workspaces } = useQuery({
    queryKey: ["workspaces"],
    queryFn: workspaceApi.list,
  });

  // Close menus on click outside
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (workspaceRef.current && !workspaceRef.current.contains(e.target as Node)) {
        setIsWorkspaceMenuOpen(false);
      }
      if (userRef.current && !userRef.current.contains(e.target as Node)) {
        setIsUserMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <>
      <header className="sticky top-0 z-40 flex h-14 w-full items-center justify-between border-b border-ink-100 dark:border-ink-700 bg-white/90 dark:bg-ink-800/90 backdrop-blur-md px-4 shadow-xs">
        {/* Left Section: Mobile toggle, Logo, Workspace Switcher */}
        <div className="flex items-center gap-3">
          <button
            onClick={onToggleSidebar}
            className="flex items-center justify-center rounded-lg p-1.5 text-ink-500 hover:bg-ink-100 dark:hover:bg-ink-700 md:hidden transition-colors"
            aria-label="Toggle sidebar"
          >
            <Menu className="h-5 w-5" />
          </button>

          <Link
            to="/workspaces"
            className="flex items-center gap-2 font-display text-base font-bold tracking-tight text-accent-600 dark:text-accent-400 hover:opacity-90 transition-opacity"
          >
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent-500 text-white shadow-sm">
              <Trello className="h-5 w-5 fill-current" />
            </div>
            <span className="hidden sm:inline-block">Mini Trello</span>
          </Link>

          {/* Workspace Switcher */}
          <div className="relative ml-2" ref={workspaceRef}>
            <button
              onClick={() => setIsWorkspaceMenuOpen((v) => !v)}
              className="flex items-center gap-1.5 rounded-lg border border-ink-200 dark:border-ink-700 bg-ink-50/60 dark:bg-ink-900/60 px-2.5 py-1.5 text-xs font-semibold text-ink-800 dark:text-ink-200 hover:bg-ink-100 dark:hover:bg-ink-700 transition-colors"
            >
              <Building2 className="h-3.5 w-3.5 text-accent-500" />
              <span className="max-w-[120px] truncate">Workspaces</span>
              <ChevronDown className="h-3.5 w-3.5 text-ink-400" />
            </button>

            {isWorkspaceMenuOpen && (
              <div className="absolute left-0 top-full mt-1.5 w-64 rounded-xl border border-ink-100 dark:border-ink-700 bg-white dark:bg-ink-800 p-2 shadow-xl animate-in fade-in zoom-in-95 duration-150 z-50">
                <div className="px-2 py-1.5 text-[11px] font-bold uppercase tracking-wider text-ink-400">
                  Your Workspaces
                </div>
                <div className="flex flex-col gap-0.5 max-h-48 overflow-y-auto">
                  {workspaces?.map((w) => (
                    <button
                      key={w.id}
                      onClick={() => {
                        setIsWorkspaceMenuOpen(false);
                        navigate(`/workspaces/${w.id}/projects`);
                      }}
                      className="flex items-center justify-between rounded-lg px-2.5 py-2 text-left text-xs font-medium text-ink-800 dark:text-ink-200 hover:bg-ink-50 dark:hover:bg-ink-700 transition-colors"
                    >
                      <span className="truncate">{w.name}</span>
                    </button>
                  ))}
                  {workspaces?.length === 0 && (
                    <span className="px-2 py-2 text-xs text-ink-400">No workspaces found</span>
                  )}
                </div>
                <div className="mt-1 border-t border-ink-100 dark:border-ink-700 pt-1">
                  <button
                    onClick={() => {
                      setIsWorkspaceMenuOpen(false);
                      navigate("/workspaces");
                    }}
                    className="flex w-full items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-xs font-medium text-accent-600 dark:text-accent-400 hover:bg-accent-50 dark:hover:bg-accent-950/40 transition-colors"
                  >
                    <Plus className="h-3.5 w-3.5" />
                    <span>Manage Workspaces</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Middle Section: Quick Search Bar */}
        <div className="flex-1 max-w-md mx-4 hidden sm:block">
          <button
            onClick={() => setIsSearchOpen(true)}
            className="flex w-full items-center gap-2 rounded-xl border border-ink-200 dark:border-ink-700 bg-ink-50/60 dark:bg-ink-900/60 px-3 py-1.5 text-xs text-ink-400 hover:bg-ink-100 dark:hover:bg-ink-700 transition-colors text-left"
          >
            <Search className="h-3.5 w-3.5 text-ink-400" />
            <span className="flex-1 truncate">Search boards, projects...</span>
            <kbd className="hidden lg:inline-block rounded border border-ink-200 dark:border-ink-600 px-1.5 py-0.5 font-mono text-[10px] text-ink-400">
              ⌘K
            </kbd>
          </button>
        </div>

        {/* Right Section: Mobile Search, Notifications & User Profile Menu */}
        <div className="flex items-center gap-2">
          {/* Mobile search icon */}
          <button
            onClick={() => setIsSearchOpen(true)}
            className="flex items-center justify-center rounded-lg p-2 text-ink-500 hover:bg-ink-100 dark:hover:bg-ink-700 sm:hidden transition-colors"
            aria-label="Search"
          >
            <Search className="h-4 w-4" />
          </button>

          {/* Notifications Bell Dropdown */}
          <NotificationBell />

          {/* User Profile Menu */}
          <div className="relative" ref={userRef}>
            <button
              onClick={() => setIsUserMenuOpen((v) => !v)}
              className="flex items-center gap-2 rounded-full p-0.5 hover:ring-2 hover:ring-accent-400 transition-all"
            >
              <Avatar name={user?.fullName || "User"} size="md" />
            </button>

            {isUserMenuOpen && (
              <div className="absolute right-0 top-full mt-2 w-56 rounded-xl border border-ink-100 dark:border-ink-700 bg-white dark:bg-ink-800 p-2 shadow-xl animate-in fade-in zoom-in-95 duration-150 z-50">
                <div className="border-b border-ink-100 dark:border-ink-700 px-3 py-2.5">
                  <p className="text-sm font-bold text-ink-900 dark:text-paper truncate">
                    {user?.fullName}
                  </p>
                  <p className="text-xs text-ink-400 truncate">{user?.email}</p>
                </div>
                <div className="flex flex-col gap-0.5 py-1">
                  <button
                    onClick={() => {
                      setIsUserMenuOpen(false);
                      void logout();
                    }}
                    className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-xs font-semibold text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/40 transition-colors"
                  >
                    <LogOut className="h-4 w-4" />
                    <span>Log out</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Global Search Modal */}
      <GlobalSearchModal isOpen={isSearchOpen} onClose={() => setIsSearchOpen(false)} />
    </>
  );
}
