import { LogOut, User as UserIcon } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/Button";

export function Topbar() {
  const { user, logout } = useAuth();

  return (
    <header className="flex h-14 items-center justify-between border-b border-ink-100 dark:border-ink-700 bg-white dark:bg-ink-800 px-4">
      <span className="font-display text-sm font-semibold tracking-tight">Mini Trello Enterprise</span>
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2 text-sm text-ink-600 dark:text-ink-200">
          <UserIcon className="h-4 w-4" aria-hidden="true" />
          <span>{user?.fullName}</span>
        </div>
        <Button variant="secondary" onClick={() => void logout()}>
          <LogOut className="h-4 w-4" aria-hidden="true" />
          Log out
        </Button>
      </div>
    </header>
  );
}
