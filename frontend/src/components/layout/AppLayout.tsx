import { useState } from "react";
import { Outlet } from "react-router-dom";
import { AppHeader } from "./AppHeader";
import { Sidebar } from "./Sidebar";
import { Breadcrumbs } from "./Breadcrumbs";

export function AppLayout() {
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);

  return (
    <div className="flex h-screen w-screen flex-col overflow-hidden bg-paper dark:bg-ink-900">
      {/* Top Header */}
      <AppHeader onToggleSidebar={() => setIsMobileSidebarOpen((v) => !v)} />

      {/* Main Body (Sidebar + Content View) */}
      <div className="flex flex-1 overflow-hidden">
        <Sidebar
          isMobileOpen={isMobileSidebarOpen}
          onCloseMobile={() => setIsMobileSidebarOpen(false)}
        />

        <div className="flex flex-1 flex-col overflow-hidden">
          <Breadcrumbs />
          <main className="flex-1 overflow-y-auto bg-ink-50/40 dark:bg-ink-900">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}
