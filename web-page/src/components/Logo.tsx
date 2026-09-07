import { motion } from "framer-motion";

export function Logo({
  size = 40,
  animated = false,
  glow = false,
  className = "",
}: {
  size?: number;
  animated?: boolean;
  glow?: boolean;
  className?: string;
}) {
  return (
    <motion.span
      className={`relative inline-flex shrink-0 items-center justify-center ${className}`}
      style={{ width: size, height: size }}
      {...(animated
        ? {
            animate: { y: [0, -8, 0] },
            transition: { duration: 6, repeat: Infinity, ease: "easeInOut" as const },
          }
        : {})}
    >
      {glow && (
        <span
          aria-hidden
          className="absolute inset-0 rounded-[28%] blur-2xl opacity-45"
          style={{ background: "var(--gradient-brand)" }}
        />
      )}
      <img
        src="/favicon.png"
        alt="Cadenza logo"
        width={size}
        height={size}
        className="relative h-full w-full object-contain drop-shadow-[0_10px_24px_rgba(99,58,220,0.25)]"
      />
    </motion.span>
  );
}

export function Wordmark({ size = 36 }: { size?: number }) {
  return (
    <span className="flex min-w-0 items-center gap-2.5">
      <Logo size={size} />
      <span className="text-[1.15rem] font-extrabold tracking-[-0.03em]">Cadenza</span>
    </span>
  );
}
