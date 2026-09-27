"use client";

import { useConverterStore } from "@/store/converterStore";
import KeySelector from "@/components/converter/KeySelector";
import SyntaxHighlighter from "@/components/converter/SyntaxHighlighter";
import ActionBar from "@/components/converter/ActionBar";
import { ArrowRightLeft } from "lucide-react";

export default function ConverterPanel() {
  const inputText = useConverterStore((s) => s.inputText);
  const outputText = useConverterStore((s) => s.outputText);
  const setInputText = useConverterStore((s) => s.setInputText);
  const fromKey = useConverterStore((s) => s.fromKey);
  const toKey = useConverterStore((s) => s.toKey);
  const setFromKey = useConverterStore((s) => s.setFromKey);
  const setToKey = useConverterStore((s) => s.setToKey);

  const handleSwapKeys = () => {
    const oldFrom = fromKey;
    const oldTo = toKey;
    setFromKey(oldTo);
    setToKey(oldFrom);
  };

  return (
    <section className="w-full max-w-5xl mx-auto px-4">
      {/* Title */}
      <div className="text-center mb-12">
        <h1 className="text-[clamp(32px,4vw,56px)] font-bold tracking-[-0.03em] leading-tight mb-4">
          Công cụ chuyển đổi cảm âm
        </h1>
        <p className="text-lg text-muted-foreground font-light">
          Nhập cảm âm, chọn tone — kết quả hiển thị ngay lập tức
        </p>
      </div>

      {/* Key selectors row */}
      <div className="flex items-center justify-center gap-4 mb-8 flex-wrap">
        <KeySelector variant="from" />

        <button
          onClick={handleSwapKeys}
          aria-label="Đổi chiều tone"
          className="
            flex items-center justify-center
            w-10 h-10 rounded-xl
            bg-card border border-border
            text-muted-foreground hover:text-foreground hover:bg-card/80 hover:border-primary/50
            transition-all duration-200
            active:scale-95
          "
        >
          <ArrowRightLeft className="w-4 h-4" />
        </button>

        <KeySelector variant="to" />
      </div>

      {/* Panels grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        {/* Input panel */}
        <div
          className="
            flex flex-col
            rounded-3xl
            border border-border
            bg-card/50
            overflow-hidden shadow-xl
          "
        >
          <div className="flex items-center justify-between px-6 py-4 border-b border-border">
            <span className="text-xs font-bold uppercase tracking-widest text-muted-foreground">
              Nhập cảm âm
            </span>
            {inputText && (
              <span className="text-[10px] text-muted-foreground/60 tabular-nums uppercase font-bold">
                {inputText.length} ký tự
              </span>
            )}
          </div>
          <textarea
            id="converter-input"
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            placeholder={"Ví dụ:\nAm  F  C  G\nDm  Am  Bb  F"}
            spellCheck={false}
            className="
              w-full flex-1 min-h-[300px] sm:min-h-[400px]
              resize-none
              bg-transparent
              px-6 py-5
              font-mono text-sm text-foreground/80
              placeholder:text-foreground/20
              focus:outline-none
              leading-relaxed
            "
          />
        </div>

        {/* Output panel */}
        <div
          className="
            flex flex-col
            rounded-3xl
            border border-border
            bg-gradient-to-br from-card to-background
            overflow-hidden shadow-xl
          "
        >
          <div className="flex items-center justify-between px-6 py-4 border-b border-border">
            <span className="text-xs font-bold uppercase tracking-widest text-primary">
              Kết quả
            </span>
            {outputText && (
              <span className="text-[10px] text-primary/60 tabular-nums uppercase font-bold">
                {outputText.length} ký tự
              </span>
            )}
          </div>
          <div className="w-full flex-1 min-h-[300px] sm:min-h-[400px] px-6 py-5 overflow-auto">
            <SyntaxHighlighter text={outputText} />
          </div>
        </div>
      </div>

      {/* Action bar */}
      <ActionBar />
    </section>
  );
}
