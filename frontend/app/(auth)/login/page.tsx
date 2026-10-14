import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import LoginForm from "@/components/auth/LoginForm";

export const metadata: Metadata = { title: "Đăng nhập · ToneShift" };

export default async function LoginPage() {
  const session = await getSession();
  if (session) {
    redirect("/");
  }

  return (
    <>
      <h1 className="text-xl font-bold text-center mb-6">Đăng nhập</h1>
      <LoginForm />
    </>
  );
}
