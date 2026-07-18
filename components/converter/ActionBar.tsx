"use client";

import { useConverterStore } from "@/store/converterStore";
import { Copy, Check, Trash2, Download } from "lucide-react";
import Button from "@/components/ui/Button";
import { useState, useCallback } from "react";

export default function ActionBar() {
  const outputText = useConverterStore((s) => s.outputText);
  const inputText = useConverterStore((s) => s.inputText);
  const clearAll = useConverterStore((s) => s.clearAll);
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback(async () => {
    if (!outputText) return;
    try {
      await navigator.clipboard.writeText(outputText);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback for older browsers
      const textarea = document.createElement("textarea");
      textarea.value = outputText;
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand("copy");
      document.body.removeChild(textarea);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  }, [outputText]);

  const handleClear = useCallback(() => {
    clearAll();
    setCopied(false);
  }, [clearAll]);

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
  }, [outputText]);

  const hasContent = !!inputText.trim();

  return (
    <div className="flex items-center gap-2">
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
    </div>
  );
}
