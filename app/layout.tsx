import type { Metadata } from "next";
import { IBM_Plex_Mono } from "next/font/google";
import "@fontsource/mona-sans";
import "./globals.css";
import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";

const ibmPlexMono = IBM_Plex_Mono({
  variable: "--font-ibm-plex-mono",
  subsets: ["latin", "vietnamese"],
  weight: ["400", "500", "600", "700"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "ToneShift – Thay đổi cảm âm của bạn",
  description:
    "ToneShift là công cụ trực tuyến giúp bạn thay đổi cảm âm bài hát yêu thích. " +
    "Dễ dàng điều chỉnh cao độ để phù hợp với tone nhạc bạn đang chơi.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="vi"
      className={`${ibmPlexMono.variable} h-full antialiased`}
    >
      <body className="bg-background text-foreground overflow-x-hidden">
        <Header />
        <main className="pt-20 min-h-[calc(100dvh-72px)]">{children}</main>
        <Footer />
      </body>
    </html>
  );
}
