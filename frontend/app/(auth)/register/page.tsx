import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import RegisterForm from "@/components/auth/RegisterForm";

export const metadata: Metadata = { title: "Tạo tài khoản · ToneShift" };

export default async function RegisterPage() {
  const session = await getSession();
  if (session) {
    redirect("/");
  }

  return (
    <>
      <h1 className="text-xl font-bold text-center mb-6">Tạo tài khoản</h1>
      <RegisterForm />
    </>
  );
}
