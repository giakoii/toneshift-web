"use client";

import { parseTokens, type Token } from "@/lib/transpose";
import { useMemo } from "react";

interface SyntaxHighlighterProps {
  text: string;
  className?: string;
}

export default function SyntaxHighlighter({ text, className = "" }: SyntaxHighlighterProps) {
  const tokens = useMemo(() => parseTokens(text), [text]);

  if (!text) {
    return (
      <div className={`text-white/20 italic select-none ${className}`}>
        Kết quả sẽ hiển thị ở đây...
      </div>
    );
  }

  // Preserve line breaks
  const lines = splitTokensByLine(tokens, text);

  return (
    <div className={`whitespace-pre-wrap font-mono text-sm leading-relaxed ${className}`}>
      {lines.map((lineTokens, lineIdx) => (
        <div key={lineIdx}>
          {lineTokens.length === 0 ? (
            <br />
          ) : (
            lineTokens.map((token, tokenIdx) => (
              <span key={tokenIdx} className={getTokenClass(token)}>
                {token.value}
              </span>
            ))
          )}
        </div>
      ))}
    </div>
  );
}

function getTokenClass(token: Token): string {
  switch (token.type) {
    case "note":
      return "text-blue-400 font-bold";
    case "separator":
      return "text-white/30";
    case "text":
    default:
      return "text-foreground";
  }
}

/**
 * Split a flat token array into lines by detecting newline characters inside text tokens.
 */
function splitTokensByLine(tokens: Token[], _originalText: string): Token[][] {
  const lines: Token[][] = [[]];

  for (const token of tokens) {
    if (token.type === "text" && token.value.includes("\n")) {
      const parts = token.value.split("\n");
      parts.forEach((part, i) => {
        if (i > 0) lines.push([]);
        if (part) {
          lines[lines.length - 1].push({ type: "text", value: part });
        }
      });
    } else {
      lines[lines.length - 1].push(token);
    }
  }

  return lines;
}
