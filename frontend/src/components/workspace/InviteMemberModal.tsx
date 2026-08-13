import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { UserPlus, Mail, Trash2, Users } from "lucide-react";
import type { AxiosError } from "axios";
import toast from "react-hot-toast";
import { projectApi } from "@/api/projects";
import type { ApiEnvelope } from "@/api/axiosClient";
import { Modal } from "@/components/ui/Modal";
import { Button } from "@/components/ui/Button";
import { Avatar } from "@/components/ui/Avatar";
import { Badge } from "@/components/ui/Badge";
import type { ProjectRole } from "@/types";

interface InviteMemberModalProps {
  projectId: string;
  isOpen: boolean;
  onClose: () => void;
}

const ROLES: ProjectRole[] = ["OWNER", "MANAGER", "CONTRIBUTOR", "VIEWER"];

export function InviteMemberModal({ projectId, isOpen, onClose }: InviteMemberModalProps) {
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<ProjectRole>("CONTRIBUTOR");
  const queryClient = useQueryClient();

  const membersQuery = useQuery({
    queryKey: ["project-members", projectId],
    queryFn: () => projectApi.listMembers(projectId),
    enabled: isOpen && !!projectId,
  });

  const inviteMutation = useMutation({
    mutationFn: (data: { email: string; role: ProjectRole }) =>
      projectApi.inviteMember(projectId, data),
    onSuccess: () => {
      toast.success(`Invited ${email} to project`);
      setEmail("");
      void queryClient.invalidateQueries({ queryKey: ["project-members", projectId] });
    },
    onError: (err) => {
      const msg = (err as AxiosError<ApiEnvelope<never>>).response?.data?.message ||
        "Could not invite member. Make sure the user is registered.";
      toast.error(msg);
    },
  });

  const removeMutation = useMutation({
    mutationFn: (userId: string) => projectApi.removeMember(projectId, userId),
    onSuccess: () => {
      toast.success("Member removed");
      void queryClient.invalidateQueries({ queryKey: ["project-members", projectId] });
    },
    onError: () => toast.error("Could not remove member."),
  });

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={
        <div className="flex items-center gap-2">
          <Users className="h-5 w-5 text-accent-500" />
          <span>Project Members & Permissions</span>
        </div>
      }
      subtitle="Invite team members to collaborate on this project"
      size="lg"
    >
      <div className="flex flex-col gap-6">
        {/* Invite Form */}
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (email.trim()) {
              inviteMutation.mutate({ email: email.trim(), role });
            }
          }}
          className="flex flex-col gap-3 rounded-xl border border-ink-100 dark:border-ink-700 bg-ink-50/50 dark:bg-ink-900/50 p-4"
        >
          <h4 className="text-xs font-bold uppercase tracking-wider text-ink-500 dark:text-ink-400">
            Invite New Member
          </h4>
          <div className="flex flex-col sm:flex-row gap-2">
            <div className="relative flex-1">
              <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" />
              <input
                type="email"
                required
                className="input-field pl-9"
                placeholder="User email address..."
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            <select
              className="input-field sm:w-36"
              value={role}
              onChange={(e) => setRole(e.target.value as ProjectRole)}
            >
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
            <Button type="submit" isLoading={inviteMutation.isPending}>
              <UserPlus className="h-4 w-4" />
              <span>Invite</span>
            </Button>
          </div>
        </form>

        {/* Existing Members List */}
        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-ink-500 dark:text-ink-400 mb-3">
            Current Members ({membersQuery.data?.length ?? 0})
          </h4>
          <div className="flex flex-col divide-y divide-ink-100 dark:divide-ink-700 rounded-xl border border-ink-100 dark:border-ink-700 overflow-hidden">
            {membersQuery.data?.map((m) => (
              <div
                key={m.userId}
                className="flex items-center justify-between p-3.5 hover:bg-ink-50/60 dark:hover:bg-ink-700/40 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <Avatar name={m.fullName} size="md" />
                  <div>
                    <p className="text-sm font-semibold text-ink-900 dark:text-paper">
                      {m.fullName}
                    </p>
                    <p className="text-xs text-ink-400 font-mono">{m.email}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Badge variant="role" role={m.role}>
                    {m.role}
                  </Badge>

                  {m.role !== "OWNER" && (
                    <button
                      onClick={() => removeMutation.mutate(m.userId)}
                      disabled={removeMutation.isPending}
                      className="rounded-lg p-1.5 text-ink-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-950/40 transition-colors"
                      title="Remove member"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  )}
                </div>
              </div>
            ))}

            {membersQuery.data?.length === 0 && (
              <div className="py-6 text-center text-sm text-ink-400">
                No members found.
              </div>
            )}
          </div>
        </div>
      </div>
    </Modal>
  );
}
