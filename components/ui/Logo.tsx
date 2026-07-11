import Image from "next/image";
import Link from "next/link";
import logo from "@/assets/logo.png";

interface LogoProps {
  size?: number;
  showName?: boolean;
  className?: string;
  asLink?: boolean;
}

export default function Logo({
  size = 32,
  showName = true,
  className = "",
  asLink = true,
}: LogoProps) {

  const content = (
    <span className={`flex items-center gap-2.5 select-none ${className}`}>

      <Image
        src={logo}
        alt="ToneShift Logo"
        width={size}
        height={size}
        className="rounded-full"
      />
      {showName && (
        <span className="font-semibold text-[15px] tracking-tight text-white">
          Tone <span className="text-blue-400">Shift</span>
        </span>
      )}
    </span>
  );

  if (asLink) {
    return (
      <Link
        href="/"
        aria-label="ToneShift – trang chủ"
        className="group"
      >
        {content}
      </Link>
    );
  }
  return content;
}
