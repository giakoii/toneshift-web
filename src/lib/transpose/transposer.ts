import type { NoteName, ToneKey, ConvertInput, ConvertResult, ConvertedToken } from "@/types";
import { getInterval } from "@/constants/tones";
import { tokenizeLine, parseNote } from "@/lib/parser/noteParser";

/**
 * Chromatic scale mapping: semitone offset (0–11) → note name in that position.
 * We use the flat naming convention matching Vietnamese cảm âm tradition.
 */
const CHROMATIC_SCALE: NoteName[] = [
  "Do",  // 0
  "Reb", // 1
  "Re",  // 2
  "Mib", // 3
  "Mi",  // 4
  "Fa",  // 5
  "Fab", // 6  (F#/Gb)
  "Sol", // 7
  "Lab", // 8
  "La",  // 9
  "Sib", // 10
  "Si",  // 11
];

/**
 * Semitone position of each note in C-major scale context (relative to C=0).
 */
const NOTE_SEMITONE: Record<NoteName, number> = {
  Do:  0,
  Reb: 1,
  Re:  2,
  Mib: 3,
  Mi:  4,
  Fa:  5,
  Fab: 6,
  Sol: 7,
  Lab: 8,
  La:  9,
  Sib: 10,
  Si:  11,
};

/**
 * Transpose a single note name by `semitones` steps.
 * Preserves the original casing (uppercase = uppercase).
 */
export function transposeNote(
  noteName: NoteName,
  semitones: number,
  originalIsUpperCase: boolean
): string {
  const currentSemitone = NOTE_SEMITONE[noteName];
  const newSemitone = ((currentSemitone + semitones) % 12 + 12) % 12;
  const transposed = CHROMATIC_SCALE[newSemitone];

  return originalIsUpperCase
    ? transposed.toUpperCase()
    : transposed.charAt(0).toUpperCase() + transposed.slice(1).toLowerCase();
}

/**
 * Main transposition function. Processes multi-line input.
 * Preserves lyrics (lines with no notes are left untouched).
 */
export function convert(input: ConvertInput): ConvertResult {
  const { text, fromTone, toTone } = input;
  const semitones = getInterval(fromTone, toTone);
  const lines = text.split("\n");

  const allTokens: ConvertedToken[] = [];
  let position = 0;

  const outputLines = lines.map((line) => {
    const tokens = tokenizeLine(line);
    let convertedLine = "";

    for (const token of tokens) {
      if (!token.isNote) {
        allTokens.push({ original: token.text, converted: token.text, isNote: false, position });
        convertedLine += token.text;
        position += token.text.length;
        continue;
      }

      // Could be a compound note like "LaRe3" – split and re-tokenize
      const parsed = parseNote(token.text);
      if (!parsed) {
        allTokens.push({ original: token.text, converted: token.text, isNote: false, position });
        convertedLine += token.text;
        position += token.text.length;
        continue;
      }

      const convertedNote = transposeNote(parsed.note, semitones, parsed.isUpperCase);
      const convertedFull = parsed.octave ? `${convertedNote}${parsed.octave}` : convertedNote;

      allTokens.push({
        original: token.text,
        converted: convertedFull,
        isNote: true,
        position,
      });

      convertedLine += convertedFull;
      position += token.text.length;
    }

    position += 1; // newline char
    return convertedLine;
  });

  return {
    input: text,
    output: outputLines.join("\n"),
    tokens: allTokens,
    fromTone,
    toTone,
  };
}
