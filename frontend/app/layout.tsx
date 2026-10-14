import type { Metadata } from "next";
import { Plus_Jakarta_Sans } from "next/font/google";
import "./globals.css";
import Toaster from "@/components/ui/Toaster";
import { AuthProvider } from "@/components/auth/AuthProvider";
import { getSession } from "@/lib/session";

const plusJakartaSans = Plus_Jakarta_Sans({
  variable: "--font-plus-jakarta",
  subsets: ["latin", "vietnamese"],
  weight: ["300", "400", "500", "600", "700"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "ToneShift · Chuyển tone bài hát nhanh và chuẩn xác",
  description: "Dán cảm âm, chọn tone, nhận kết quả ngay. Hỗ trợ đủ 12 tone và ký hiệu Đô Rê Mi.",
};

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const session = await getSession();

  return (
    <html
      lang="vi"
      className={`${plusJakartaSans.variable} h-full antialiased dark`}
    >
      <body className="bg-background text-foreground overflow-x-hidden font-sans relative selection:bg-primary/30">
        <AuthProvider initialUser={session?.user ?? null}>
          {children}
        </AuthProvider>
        <Toaster />
      </body>
    </html>
  );
}

