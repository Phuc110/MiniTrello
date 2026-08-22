export type SystemRole = "ADMIN" | "MEMBER";
export type Priority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

export interface User {
  id: string;
  email: string;
  fullName: string;
  systemRole: SystemRole;
}

export interface AuthResponse {
  accessToken: string;
  expiresInSeconds: number;
  user: User;
}

export interface Workspace {
  id: string;
  name: string;
  slug: string;
  ownerId: string;
  createdAt: string;
  canDelete: boolean;
}

export interface Board {
  id: string;
  workspaceId: string;
  name: string;
  createdAt: string;
}

export interface BoardListColumn {
  id: string;
  boardId: string;
  name: string;
  position: string;
}

export interface TaskAssignee {
  userId: string;
  fullName: string;
  email: string;
}

export interface TagDto {
  id: string;
  name: string;
  color: string;
}

export interface Task {
  id: string;
  boardListId: string;
  /** Resolved server-side (list → board → workspace); null if the parent list was soft-deleted. */
  workspaceId?: string | null;
  title: string;
  description: string | null;
  priority: Priority;
  position: string;
  dueDate: string | null;
  assignees: TaskAssignee[];
  tags: TagDto[];
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export type NotificationType = "DEADLINE_SOON" | "DUE_TODAY" | "OVERDUE";

export interface Notification {
  id: string;
  taskId: string;
  /** Resolved server-side (task → list → board) for deep-linking; null if the task no longer exists. */
  boardId?: string | null;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  readAt: string | null;
}
