import Spinner from "@/components/ui/Spinner";

export default function Loading() {
  return (
    <div
      role="status"
      aria-live="polite"
      className="min-h-screen flex items-center justify-center"
    >
      <Spinner size="lg" className="text-primary" />
      <span className="sr-only">Đang tải...</span>
    </div>
  );
}
