"use client";

import { useConverterStore } from "@/store/converterStore";
import { Copy, Check, Trash2, Download, MonitorPlay } from "lucide-react";
import { AnimatePresence } from "framer-motion";
import PerformanceMode from "@/components/performance/PerformanceMode";
import Button from "@/components/ui/Button";
import { useState, useCallback, useEffect, useRef } from "react";

export default function ActionBar() {
  const outputText = useConverterStore((s) => s.outputText);
  const inputText = useConverterStore((s) => s.inputText);
  const clearAll = useConverterStore((s) => s.clearAll);
  const fromKey = useConverterStore((s) => s.fromKey);
  const toKey = useConverterStore((s) => s.toKey);
  const [performing, setPerforming] = useState(false);
  const closePerformance = useCallback(() => setPerforming(false), []);
  const [copied, setCopied] = useState(false);
  const [feedback, setFeedback] = useState<{ type: "ok" | "error"; text: string } | null>(null);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const showFeedback = useCallback((type: "ok" | "error", text: string) => {
    if (timerRef.current) clearTimeout(timerRef.current);
    setFeedback({ type, text });
    timerRef.current = setTimeout(() => {
      setFeedback(null);
      setCopied(false);
    }, 2000);
  }, []);

  useEffect(() => () => {
    if (timerRef.current) clearTimeout(timerRef.current);
  }, []);

  const handleCopy = useCallback(async () => {
    if (!outputText) return;
    try {
      await navigator.clipboard.writeText(outputText);
      setCopied(true);
      showFeedback("ok", "Đã copy kết quả");
    } catch {
      showFeedback("error", "Không thể copy. Hãy chọn và copy thủ công.");
    }
  }, [outputText, showFeedback]);

  const handleClear = useCallback(() => {
    if (!window.confirm("Xoá toàn bộ nội dung và đặt lại tone?")) return;
    clearAll();
    setCopied(false);
    showFeedback("ok", "Đã xoá nội dung");
  }, [clearAll, showFeedback]);

  const handleDownload = useCallback(() => {
    if (!outputText) return;
    const blob = new Blob([outputText], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "toneshift-output.txt";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    showFeedback("ok", "Đã tải xuống toneshift-output.txt");
  }, [outputText, showFeedback]);

  const hasContent = !!inputText.trim();

  return (
    <div className="flex items-center gap-2 flex-wrap">
      <Button
        variant="secondary"
        size="sm"
        id="action-copy"
        onClick={handleCopy}
        disabled={!outputText}
        leftIcon={
          copied
            ? <Check className="w-3.5 h-3.5 text-green-400" />
            : <Copy className="w-3.5 h-3.5" />
        }
        className={copied ? "border border-green-500/30 bg-green-500/10" : ""}
      >
        {copied ? "Đã copy" : "Copy"}
      </Button>

      <Button
        variant="ghost"
        size="sm"
        id="action-clear"
        onClick={handleClear}
        disabled={!hasContent}
        leftIcon={<Trash2 className="w-3.5 h-3.5" />}
      >
        Xoá
      </Button>

      <Button
        variant="ghost"
        size="sm"
        id="action-download"
        onClick={handleDownload}
        disabled={!outputText}
        leftIcon={<Download className="w-3.5 h-3.5" />}
      >
        Tải xuống
      </Button>

      <Button
        variant="primary"
        size="sm"
        id="action-perform"
        onClick={() => setPerforming(true)}
        disabled={!outputText}
        leftIcon={<MonitorPlay className="w-3.5 h-3.5" />}
      >
        Biểu diễn
      </Button>

      <span
        role="status"
        aria-live="polite"
        className={`text-xs ${feedback?.type === "error" ? "text-red-400" : "text-muted-foreground"}`}
      >
        {feedback?.text}
      </span>

      <AnimatePresence>
        {performing && (
          <PerformanceMode
            title="Bản cảm âm của bạn"
            text={outputText}
            toneLabel={`Tone ${fromKey} → ${toKey}`}
            onClose={closePerformance}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
