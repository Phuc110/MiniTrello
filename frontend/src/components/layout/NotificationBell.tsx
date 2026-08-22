import { useState, useRef, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Bell, CheckCheck } from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import toast from "react-hot-toast";
import { notificationApi } from "@/api/notifications";
import type { Notification, NotificationType } from "@/types";

dayjs.extend(relativeTime);

export function NotificationBell() {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  // Non-null only while the header is rendered on a board page — lets the
  // click handler detect "already viewing this board" and skip the redirect.
  const { boardId: currentBoardId } = useParams<{ boardId: string }>();

  // Query all notifications, auto-refetching every 60 seconds
  const { data: notifications = [] } = useQuery({
    queryKey: ["notifications"],
    queryFn: notificationApi.listAll,
    refetchInterval: 60_000,
  });

  const unreadCount = notifications.filter((n) => !n.read).length;

  const markReadMutation = useMutation({
    mutationFn: notificationApi.markRead,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });

  const markAllReadMutation = useMutation({
    mutationFn: notificationApi.markAllRead,
    onSuccess: () => {
      toast.success("All notifications marked as read");
      void queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });

  // Close dropdown on click outside
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const typeConfig: Record<NotificationType, { icon: string; bg: string }> = {
    OVERDUE: { icon: "🔴", bg: "bg-red-50 dark:bg-red-950/30 text-red-700 dark:text-red-300 border-red-200 dark:border-red-800" },
    DUE_TODAY: { icon: "🟠", bg: "bg-amber-50 dark:bg-amber-950/30 text-amber-700 dark:text-amber-300 border-amber-200 dark:border-amber-800" },
    DEADLINE_SOON: { icon: "🟡", bg: "bg-yellow-50 dark:bg-yellow-950/30 text-yellow-700 dark:text-yellow-300 border-yellow-200 dark:border-yellow-800" },
  };

  const handleNotificationClick = (notification: Notification) => {
    // Step 1 — mark as read: optimistic cache patch drops the badge counter
    // instantly; the mutation keeps the server in sync afterwards.
    if (!notification.read) {
      queryClient.setQueryData<Notification[]>(["notifications"], (old) =>
        old?.map((n) => (n.id === notification.id ? { ...n, read: true } : n))
      );
      markReadMutation.mutate(notification.id);
    }

    // Step 2 — navigate / open the task detail.
    if (!notification.boardId || !notification.taskId) {
      // Task no longer exists — nothing to deep-link to.
      toast("The related task is no longer available.");
    } else if (currentBoardId === notification.boardId) {
      // Already viewing the right board: no redirect/reload needed — just
      // sync the ?taskId= param, BoardPage's deep-link effect opens the modal.
      navigate(`/boards/${currentBoardId}?taskId=${notification.taskId}`, {
        replace: true,
      });
    } else {
      navigate(`/boards/${notification.boardId}?taskId=${notification.taskId}`);
    }

    // Step 3 — close the popover.
    setIsOpen(false);
  };

  return (
    <div className="relative" ref={containerRef}>
      {/* Bell trigger button */}
      <button
        onClick={() => setIsOpen((prev) => !prev)}
        className="relative flex h-9 w-9 items-center justify-center rounded-xl text-ink-500 hover:bg-ink-100 dark:hover:bg-ink-700 hover:text-ink-900 dark:hover:text-paper transition-colors"
        aria-label="Notifications"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white shadow-xs animate-pulse">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown panel */}
      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-80 sm:w-96 rounded-2xl border border-ink-100 dark:border-ink-700 bg-white dark:bg-ink-800 shadow-2xl z-50 overflow-hidden animate-in fade-in zoom-in-95 duration-150">
          {/* Panel Header */}
          <div className="flex items-center justify-between border-b border-ink-100 dark:border-ink-700 px-4 py-3 bg-ink-50/50 dark:bg-ink-900/50">
            <div className="flex items-center gap-2">
              <span className="font-display text-sm font-bold text-ink-900 dark:text-paper">
                Notifications
              </span>
              {unreadCount > 0 && (
                <span className="rounded-full bg-accent-100 dark:bg-accent-950 px-2 py-0.5 text-xs font-bold text-accent-700 dark:text-accent-300">
                  {unreadCount} new
                </span>
              )}
            </div>

            {unreadCount > 0 && (
              <button
                onClick={() => markAllReadMutation.mutate()}
                disabled={markAllReadMutation.isPending}
                className="flex items-center gap-1 text-xs font-semibold text-accent-600 dark:text-accent-400 hover:underline disabled:opacity-50"
              >
                <CheckCheck className="h-3.5 w-3.5" />
                <span>Mark all as read</span>
              </button>
            )}
          </div>

          {/* Notifications List */}
          <div className="max-h-80 overflow-y-auto divide-y divide-ink-100 dark:divide-ink-700/60">
            {notifications.length === 0 ? (
              <div className="py-8 text-center text-xs text-ink-400">
                No notifications right now.
              </div>
            ) : (
              notifications.map((item) => {
                const style = typeConfig[item.type] || typeConfig.DEADLINE_SOON;
                return (
                  <div
                    key={item.id}
                    onClick={() => handleNotificationClick(item)}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") handleNotificationClick(item);
                    }}
                    className={`flex cursor-pointer items-start gap-3 p-3.5 transition-colors ${
                      item.read
                        ? "bg-white dark:bg-ink-800 opacity-75 hover:bg-ink-50/60 dark:hover:bg-ink-700/50"
                        : "bg-accent-50/30 dark:bg-accent-950/20 hover:bg-accent-50/60 dark:hover:bg-accent-950/40"
                    }`}
                  >
                    <span className="text-base select-none mt-0.5">{style.icon}</span>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-xs font-bold text-ink-900 dark:text-paper truncate">
                          {item.title}
                        </span>
                        {!item.read && (
                          <span className="h-2 w-2 rounded-full bg-accent-500 shrink-0" />
                        )}
                      </div>

                      <p className="text-xs text-ink-600 dark:text-ink-300 mt-0.5 leading-snug">
                        {item.message}
                      </p>

                      <span className="text-[10px] text-ink-400 font-mono mt-1 block">
                        {dayjs(item.createdAt).fromNow()}
                      </span>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}
    </div>
  );
}
