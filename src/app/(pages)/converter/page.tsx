import type { Metadata } from "next";
import Container from "@/app/components/Container";
import {ArchiveX, ChevronDown, Copy, Download, FileText, Info, Save} from "lucide-react";
export const metadata: Metadata = {
  title: "Chuyển Tone Cảm Âm – ToneShift",
  description:
    "Chuyển đổi cảm âm giữa các tone nhạc tự động. Hỗ trợ sáo trúc, harmonica, guitar, bolero, cải lương.",
};



export default function ConverterPage() {
  return (
    <Container>
      <main className="mx-auto max-w-5xl px-4 pt-12 pb-24">

        {/* HERO SECTION */}
        <div className="mb-12 text-center">
          <h1 className="mb-4 text-3xl font-bold tracking-tight md:text-4xl lg:text-5xl">
            Convert Vietnamese Music Notation Instantly
          </h1>
          <p className="text-zinc-400 md:text-lg">
            Transpose <span className="italic text-zinc-300">*cảm âm*</span> (Re Mi Fa...) between musical keys automatically.
          </p>
        </div>

        {/* CONVERTER WORKSPACE */}
        <div className="grid gap-6 md:grid-cols-2">

          {/* LEFT COLUMN: INPUT */}
          <div className="flex flex-col gap-3">
            {/* Editor Card */}
            <div className="flex min-h-80 flex-col mt-12 rounded-xl border border-white/10 bg-[#18181B] p-4 relative overflow-hidden group">
              <div className="mb-4 flex items-center justify-between">
                <h3 className="font-medium text-white">Input Notation (D Major)</h3>
                <div className="flex items-center gap-2">
                  <span className="rounded bg-white/5 px-2 py-1 text-xs text-zinc-400 border border-white/5">Textarea</span>
                  <Info size={16} className="text-zinc-500" />
                </div>
              </div>

              {/* Fake Textarea with Syntax Highlighting for UI mockup */}
              <div className="flex-1 font-mono text-base leading-relaxed tracking-wide text-zinc-300 focus:outline-none">
                <p>Từ là từ phu tướng</p>
                <p className="mt-3">
                  <span className="text-purple-400">Re</span>{' '}
                  <span className="text-purple-400">Re</span>{' '}
                  <span className="text-blue-400">la</span>{' '}
                  <span className="text-yellow-400">Sol</span>{' '}
                  <span className="text-blue-400">La</span>{' '}
                  <span className="text-zinc-300">Si</span>
                </p>
                <p className="mt-3">
                  <span className="text-zinc-300">Sol</span>{' '}
                  <span className="text-blue-400">La</span>{' '}
                  <span className="text-purple-400">Do</span>{' '}
                  <span className="text-blue-400">Re</span>{' '}
                  <span className="text-purple-400">Do</span>{' '}
                  <span className="text-zinc-300">Si</span>{' '}
                  <span className="text-yellow-400">Sol</span>
                </p>
                <p className="mt-3 text-zinc-100">Dẫu có dẫu có tình tang</p>
              </div>

              {/* Bottom Actions */}
              <div className="mt-4 flex items-center justify-between border-t border-white/5 pt-4">
                <div className="flex gap-2">
                  <button className="flex items-center gap-1.5 rounded-md border border-white/10 px-3 py-1.5 text-xs text-zinc-300 hover:bg-white/5 transition-colors">
                    <ArchiveX size={14} /> Clear
                  </button>
                  <button className="flex items-center gap-1.5 rounded-md border border-white/5 px-3 py-1.5 text-xs text-zinc-500 cursor-not-allowed">
                    <Save size={14} /> Save
                  </button>
                </div>
                <div className="flex items-center gap-1.5 text-xs text-zinc-400">
                  <Info size={14} /> Detected: D Major
                </div>
              </div>
            </div>
          </div>

          {/* RIGHT COLUMN: OUTPUT */}
          <div className="flex flex-col gap-3">
            {/* Target Select */}
            <div className="flex h-10 w-full items-center justify-between rounded-md border border-white/10 bg-[#18181B] px-3 text-sm text-zinc-300 cursor-pointer hover:bg-white/5">
              <span>Target Key: C Major / Đô Trưởng</span>
              <input type="number" size={16} className="text-zinc-500" />
            </div>

            {/* Output Card */}
            <div className="flex min-h-80 flex-col rounded-xl border-2 border-blue-500/20 bg-[#18181B] p-4 relative shadow-[0_0_30px_rgba(37,99,235,0.05)]">
              <div className="mb-4">
                <h3 className="font-medium text-white">Converted Output (C Major)</h3>
              </div>

              {/* Output Content */}
              <div className="flex-1 rounded-md bg-[#09090B]/50 p-4 font-mono text-base leading-relaxed tracking-wide text-zinc-300 border border-white/5">
                <p>Từ là từ phu tướng</p>
                <p className="mt-3">
                  <span className="text-blue-400">Do</span>{' '}
                  <span className="text-blue-400">Do</span>{' '}
                  <span className="text-yellow-400">sol</span>{' '}
                  <span className="text-blue-500">Fa</span>{' '}
                  <span className="text-yellow-400">Sol</span>{' '}
                  <span className="text-blue-400">La</span>
                </p>
                <p className="mt-3">
                  <span className="text-blue-500">Fa</span>{' '}
                  <span className="text-yellow-400">Sol</span>{' '}
                  <span className="text-yellow-400">Sib</span>{' '}
                  <span className="text-blue-400">Do</span>{' '}
                  <span className="text-yellow-400">Sib</span>{' '}
                  <span className="text-blue-400">La</span>{' '}
                  <span className="text-blue-500">Fa</span>
                </p>
                <p className="mt-3 text-zinc-100">Dẫu có dẫu có tình tang</p>
              </div>

              {/* Bottom Actions (Primary buttons) */}
              <div className="mt-4 flex flex-wrap items-center gap-3">
                <button className="flex flex-1 items-center justify-center gap-2 rounded-md bg-[#2563EB] py-2 text-sm font-medium text-white hover:bg-blue-600 transition-colors shadow-lg shadow-blue-900/20">
                  <Copy size={16} /> Copy
                </button>
                <button className="flex flex-1 items-center justify-center gap-2 rounded-md bg-[#2563EB] py-2 text-sm font-medium text-white hover:bg-blue-600 transition-colors shadow-lg shadow-blue-900/20">
                  <Download size={16} /> Download TXT
                </button>
                <button className="flex flex-1 items-center justify-center gap-2 rounded-md bg-[#2563EB] py-2 text-sm font-medium text-white hover:bg-blue-600 transition-colors shadow-lg shadow-blue-900/20">
                  <FileText size={16} /> Export PDF
                </button>
              </div>
            </div>
          </div>

        </div>
      </main>
    </Container>
  );
}
