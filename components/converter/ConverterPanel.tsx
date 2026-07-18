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
      <div className="text-center mb-8">
        <h1 className="text-2xl sm:text-3xl font-bold text-white mb-2">
          Chuyển đổi cảm âm
        </h1>
        <p className="text-sm text-white/40">
          Nhập cảm âm, chọn tone — kết quả hiển thị ngay lập tức
        </p>
      </div>

      {/* Key selectors row */}
      <div className="flex items-center justify-center gap-3 mb-6 flex-wrap">
        <KeySelector variant="from" />

        <button
          onClick={handleSwapKeys}
          aria-label="Đổi chiều tone"
          className="
            flex items-center justify-center
            w-8 h-8 rounded-lg
            bg-white/[0.05] border border-white/[0.08]
            text-white/40 hover:text-white hover:bg-white/[0.1]
            transition-all duration-200
            active:scale-90
          "
        >
          <ArrowRightLeft className="w-3.5 h-3.5" />
        </button>

        <KeySelector variant="to" />
      </div>

      {/* Panels grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
        {/* Input panel */}
        <div
          className="
            flex flex-col
            rounded-2xl
            border border-white/[0.08]
            bg-white/[0.03]
            overflow-hidden
          "
        >
          <div className="flex items-center justify-between px-4 py-2.5 border-b border-white/[0.06]">
            <span className="text-xs font-medium text-white/30 uppercase tracking-wider">
              Nhập cảm âm
            </span>
            {inputText && (
              <span className="text-[10px] text-white/20 tabular-nums">
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
              w-full flex-1 min-h-[200px] sm:min-h-[280px]
              resize-none
              bg-transparent
              px-4 py-3
              font-mono text-sm text-foreground
              placeholder:text-white/15
              focus:outline-none
              leading-relaxed
            "
          />
        </div>

        {/* Output panel */}
        <div
          className="
            flex flex-col
            rounded-2xl
            border border-white/[0.08]
            bg-blue-500/[0.03]
            overflow-hidden
          "
        >
          <div className="flex items-center justify-between px-4 py-2.5 border-b border-white/[0.06]">
            <span className="text-xs font-medium text-blue-400/60 uppercase tracking-wider">
              Kết quả
            </span>
            {outputText && (
              <span className="text-[10px] text-white/20 tabular-nums">
                {outputText.length} ký tự
              </span>
            )}
          </div>
          <div className="w-full flex-1 min-h-[200px] sm:min-h-[280px] px-4 py-3 overflow-auto">
            <SyntaxHighlighter text={outputText} />
          </div>
        </div>
      </div>

      {/* Action bar */}
      <ActionBar />
    </section>
  );
}
