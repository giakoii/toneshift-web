import type { Metadata } from "next";
import { Plus_Jakarta_Sans } from "next/font/google";
import "./globals.css";

const plusJakartaSans = Plus_Jakarta_Sans({
  variable: "--font-plus-jakarta",
  subsets: ["latin"],
  weight: ["300", "500", "700"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "HUSH · Sleep, without the scrolling",
  description: "Hush turns your phone into the one thing in the bedroom that helps you sleep.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${plusJakartaSans.variable} h-full antialiased dark`}
    >
      <body className="bg-background text-foreground overflow-x-hidden font-sans relative selection:bg-primary/30">
        {children}
      </body>
    </html>
  );
}

