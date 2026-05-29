import "./globals.css";
import { Inter } from "next/font/google";
import type { Metadata } from "next";
import Header from "@/app/components/ui/Header";
import Footer from "@/app/components/ui/Footer";
import { ThemeProvider } from "@/app/components/providers/ThemeProvider";

const inter = Inter({
  subsets: ["vietnamese", "latin"],
  weight: ["300", "400", "500", "600", "700"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "ToneShift – Chuyển Tone Cảm Âm Việt Nam",
  description:
    "Công cụ chuyển đổi cảm âm nhạc Việt Nam hàng đầu. Transpose cảm âm sáo trúc, harmonica, guitar giữa các tone một cách tự động, nhanh chóng và chính xác.",
  keywords: [
    "đổi tone cảm âm",
    "chuyển tone nhạc",
    "cảm âm sáo trúc",
    "transpose Vietnamese music",
    "cảm âm cải lương",
    "đổi tone guitar",
  ],
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="vi" suppressHydrationWarning>
      <body suppressHydrationWarning className={`${inter.className} flex flex-col min-h-screen bg-background text-foreground`}>
        <ThemeProvider
          attribute="class"
          defaultTheme="dark"
          enableSystem
          disableTransitionOnChange={false}
          storageKey="toneshift_theme"
        >
          <Header />
          <main className="flex-1 pt-16">{children}</main>
          <Footer />
        </ThemeProvider>
      </body>
    </html>
  );
}