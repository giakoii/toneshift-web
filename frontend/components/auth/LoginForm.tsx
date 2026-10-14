"use client";

import { useActionState } from "react";
import { useFormStatus } from "react-dom";
import Link from "next/link";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import GoogleButton from "@/components/auth/GoogleButton";
import { loginAction, type AuthFormState } from "@/app/(auth)/actions";
import { fieldError } from "@/lib/form-errors";

const INITIAL_STATE: AuthFormState = {};

function SubmitButton() {
  const { pending } = useFormStatus();
  return (
    <Button type="submit" className="w-full" isLoading={pending}>
      Đăng nhập
    </Button>
  );
}

export default function LoginForm() {
  const [state, formAction] = useActionState(loginAction, INITIAL_STATE);

  return (
    <form action={formAction} className="flex flex-col gap-4">
      <Input
        label="Email"
        name="email"
        type="email"
        autoComplete="email"
        required
        error={fieldError(state.fieldErrors, "email")}
      />
      <Input
        label="Mật khẩu"
        name="password"
        type="password"
        autoComplete="current-password"
        required
        error={fieldError(state.fieldErrors, "password")}
      />

      {state.error && <p className="text-sm text-red-400" role="alert">{state.error}</p>}

      <SubmitButton />

      <div className="relative text-center text-sm text-muted-foreground my-2">
        <span className="bg-card px-2 relative z-10">hoặc</span>
        <div className="absolute inset-x-0 top-1/2 h-px bg-border -z-0" />
      </div>

      <GoogleButton />

      <p className="text-sm text-muted-foreground text-center mt-2">
        Chưa có tài khoản?{" "}
        <Link href="/register" className="text-primary hover:underline">
          Đăng ký
        </Link>
      </p>
    </form>
  );
}
