import { motion } from "framer-motion";

/** Lightweight 2D fallback / decorative waveform (also used when WebGL is unavailable). */
export function AudioVisualizer({
  bars = 40,
  className = "",
  color = "var(--primary)",
}: {
  bars?: number;
  className?: string;
  color?: string;
}) {
  return (
    <div className={`flex h-16 items-end justify-center gap-[3px] ${className}`} aria-hidden>
      {Array.from({ length: bars }).map((_, i) => {
        const h = 18 + Math.abs(Math.sin(i * 0.55)) * 70 + Math.abs(Math.sin(i * 0.19)) * 20;
        return (
          <motion.span
            key={i}
            className="w-[3px] rounded-full"
            style={{ background: color, opacity: 0.25 + (h / 110) * 0.65 }}
            initial={{ height: 6 }}
            animate={{ height: [h * 0.35, h, h * 0.5] }}
            transition={{
              duration: 1.6 + (i % 5) * 0.25,
              repeat: Infinity,
              repeatType: "mirror",
              ease: "easeInOut",
              delay: i * 0.03,
            }}
          />
        );
      })}
    </div>
  );
}

export function HeroFallback() {
  return (
    <div className="relative flex h-full w-full items-center justify-center">
      <div
        className="absolute h-56 w-56 rounded-full blur-3xl opacity-40 sm:h-80 sm:w-80"
        style={{ background: "var(--gradient-brand)" }}
      />
      <AudioVisualizer bars={28} className="relative h-40 w-full max-w-md" />
    </div>
  );
}
