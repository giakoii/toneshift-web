"use client";

import { useEffect, useId, useRef, useTransition, useCallback } from "react";
import Script from "next/script";
import { googleLoginAction } from "@/app/(auth)/actions";
import { toast } from "@/store/toastStore";

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string;
            callback: (response: { credential: string }) => void;
          }) => void;
          renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void;
        };
      };
    };
  }
}

export default function GoogleButton() {
  const containerId = useId();
  const containerRef = useRef<HTMLDivElement>(null);
  const [isPending, startTransition] = useTransition();

  const renderGoogleButton = useCallback(() => {
    const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
    if (!clientId || !window.google || !containerRef.current) return;

    window.google.accounts.id.initialize({
      client_id: clientId,
      callback: (response) => {
        startTransition(async () => {
          const result = await googleLoginAction(response.credential);
          if (result?.error) {
            toast.error(result.error);
          }
        });
      },
    });

    window.google.accounts.id.renderButton(containerRef.current, {
      type: "standard",
      theme: "outline",
      size: "large",
      width: 320,
      text: "continue_with",
      locale: "vi",
    });
  }, []);

  useEffect(() => {
    renderGoogleButton();
  }, [renderGoogleButton]);

  return (
    <>
      <Script
        src="https://accounts.google.com/gsi/client"
        strategy="afterInteractive"
        onLoad={renderGoogleButton}
      />
      <div
        id={containerId}
        ref={containerRef}
        className="flex justify-center min-h-[44px]"
        aria-busy={isPending}
      />
      {!process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID && (
        <p className="text-xs text-muted-foreground text-center">
          Chưa cấu hình NEXT_PUBLIC_GOOGLE_CLIENT_ID — nút Google sẽ không hiển thị.
        </p>
      )}
    </>
  );
}
