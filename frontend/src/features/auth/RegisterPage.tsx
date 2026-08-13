import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { useAuth } from "@/hooks/useAuth";
import { registerSchema, type RegisterFormValues } from "@/lib/validation";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import type { AxiosError } from "axios";
import type { ApiEnvelope } from "@/api/axiosClient";

export function RegisterPage() {
  const { register: registerUser } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(registerSchema) });

  const onSubmit = async (values: RegisterFormValues) => {
    setServerError(null);
    try {
      await registerUser(values);
      navigate("/workspaces", { replace: true });
      toast.success("Account created — welcome!");
    } catch (err) {
      const message =
        (err as AxiosError<ApiEnvelope<never>>).response?.data?.message ?? "Something went wrong. Please try again.";
      setServerError(message);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-paper dark:bg-ink-900 px-4">
      <div className="card w-full max-w-sm p-8">
        <h1 className="font-display text-2xl font-semibold text-ink-900 dark:text-paper">Create your account</h1>
        <p className="mt-1 text-sm text-ink-400">Start organizing your team&apos;s work</p>

        <form className="mt-6 flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
          <Input label="Full name" autoComplete="name" error={errors.fullName?.message} {...register("fullName")} />
          <Input
            label="Email"
            type="email"
            autoComplete="email"
            error={errors.email?.message}
            {...register("email")}
          />
          <Input
            label="Password"
            type="password"
            autoComplete="new-password"
            error={errors.password?.message}
            {...register("password")}
          />

          {serverError && (
            <p role="alert" className="text-sm text-priority-urgent">
              {serverError}
            </p>
          )}

          <Button type="submit" isLoading={isSubmitting} className="mt-2 w-full">
            Create account
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-ink-400">
          Already have an account?{" "}
          <Link to="/login" className="font-medium text-accent-500 hover:underline">
            Log in
          </Link>
        </p>
      </div>
    </div>
  );
}
