import type { FieldErrorItem } from "@/types/auth";

export function fieldError(errors: FieldErrorItem[] | undefined, field: string): string | undefined {
  return errors?.find((e) => e.field === field)?.message;
}
