import { motion } from "framer-motion";
import { Reveal, SectionHeading } from "@/components/Reveal";

const SCORES = [
  { label: "Pitch Accuracy", value: 92 },
  { label: "Timing", value: 88 },
  { label: "Stability", value: 91 },
  { label: "Overall", value: 90 },
] as const;

const NOTES = ["C4", "E4", "G4", "A4", "C5"];

export function VocalAnalysis() {
  return (
    <section id="features" className="section-pad">
      <div className="shell">
        <SectionHeading
          eyebrow="Live vocal analysis"
          title={<>See the take, not just the score.</>}
          subtitle="Cadenza draws your performance against the melody you were reaching for, so every correction has a shape you can hear."
        />

        <Reveal className="mt-14">
          <div className="surface-card overflow-hidden p-5 sm:p-8">
            <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-4">
              <div className="min-w-0">
                <p className="flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                  <span className="relative flex h-2 w-2">
                    <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-70" />
                    <span className="relative inline-flex h-2 w-2 rounded-full bg-primary" />
                  </span>
                  Live Vocal Analysis
                </p>
                <h3 className="mt-2 truncate text-xl font-extrabold tracking-tight sm:text-2xl">
                  Take 03 — “Falling Light”
                </h3>
              </div>
              <div className="shrink-0 rounded-2xl bg-accent/70 px-4 py-2 text-center">
                <p className="text-[0.65rem] font-semibold uppercase tracking-widest text-muted-foreground">
                  Range
                </p>
                <p className="text-sm font-bold">C3 – A5</p>
              </div>
            </div>

            {/* Pitch graph */}
            <div className="relative mt-7 overflow-hidden rounded-3xl bg-[color-mix(in_oklab,var(--lavender)_85%,white)] p-4 sm:p-6">
              <div className="flex gap-3">
                <div className="flex flex-col justify-between py-1 text-[0.65rem] font-semibold text-muted-foreground">
                  {[...NOTES].reverse().map((n) => (
                    <span key={n}>{n}</span>
                  ))}
                </div>
                <svg viewBox="0 0 600 220" className="h-44 w-full sm:h-60" role="img"
                  aria-label="Pitch graph comparing the reference melody with the singer's pitch line">
                  {[0, 1, 2, 3, 4].map((i) => (
                    <line
                      key={i}
                      x1="0"
                      x2="600"
                      y1={12 + i * 49}
                      y2={12 + i * 49}
                      stroke="color-mix(in oklab, var(--primary) 12%, transparent)"
                      strokeWidth="1"
                    />
                  ))}
                  {/* reference */}
                  <path
                    d="M8 170 L100 170 L100 122 L200 122 L200 73 L310 73 L310 122 L410 122 L410 24 L520 24 L520 73 L592 73"
                    fill="none"
                    stroke="color-mix(in oklab, var(--secondary) 45%, transparent)"
                    strokeWidth="6"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeDasharray="2 12"
                  />
                  {/* user pitch */}
                  <motion.path
                    d="M8 176 C 60 172, 82 168, 100 132 S 150 120, 200 84 S 260 70, 312 78 S 360 118, 412 40 S 470 22, 522 66 S 570 76, 592 70"
                    fill="none"
                    stroke="url(#pitchGrad)"
                    strokeWidth="5"
                    strokeLinecap="round"
                    initial={{ pathLength: 0 }}
                    whileInView={{ pathLength: 1 }}
                    viewport={{ once: true, margin: "-60px" }}
                    transition={{ duration: 2.6, ease: "easeInOut" }}
                  />
                  <defs>
                    <linearGradient id="pitchGrad" x1="0" x2="1">
                      <stop offset="0%" stopColor="oklch(0.58 0.25 300)" />
                      <stop offset="100%" stopColor="oklch(0.7 0.16 230)" />
                    </linearGradient>
                  </defs>
                  {[
                    [100, 132],
                    [200, 84],
                    [312, 78],
                    [412, 40],
                    [522, 66],
                  ].map(([cx, cy], i) => (
                    <motion.circle
                      key={i}
                      cx={cx}
                      cy={cy}
                      r="5"
                      fill="var(--card)"
                      stroke="var(--primary)"
                      strokeWidth="3"
                      initial={{ opacity: 0, scale: 0 }}
                      whileInView={{ opacity: 1, scale: 1 }}
                      viewport={{ once: true }}
                      transition={{ delay: 0.5 + i * 0.4, duration: 0.4 }}
                    />
                  ))}
                  <motion.line
                    y1="0"
                    y2="220"
                    stroke="color-mix(in oklab, var(--primary) 45%, transparent)"
                    strokeWidth="2"
                    initial={{ x1: 8, x2: 8 }}
                    whileInView={{ x1: 592, x2: 592 }}
                    viewport={{ once: true }}
                    transition={{ duration: 2.6, ease: "easeInOut" }}
                  />
                </svg>
              </div>
              <div className="mt-3 flex flex-wrap gap-4 pl-8 text-xs font-medium text-muted-foreground">
                <span className="flex items-center gap-2">
                  <span className="h-1 w-6 rounded-full bg-secondary/50" /> Reference melody
                </span>
                <span className="flex items-center gap-2">
                  <span className="h-1 w-6 rounded-full bg-brand" /> Your pitch
                </span>
              </div>
            </div>

            <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
              {SCORES.map((s, i) => (
                <Reveal key={s.label} delay={i * 0.08}>
                  <div className="rounded-3xl border border-border bg-card p-5">
                    <p className="text-xs font-semibold uppercase tracking-[0.14em] text-muted-foreground">
                      {s.label}
                    </p>
                    <p className="mt-2 text-3xl font-extrabold tracking-tight text-gradient">
                      {s.value}%
                    </p>
                    <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-accent">
                      <motion.span
                        className="block h-full rounded-full bg-brand"
                        initial={{ width: 0 }}
                        whileInView={{ width: `${s.value}%` }}
                        viewport={{ once: true }}
                        transition={{ duration: 1.2, delay: 0.2 + i * 0.1, ease: [0.22, 1, 0.36, 1] }}
                      />
                    </div>
                  </div>
                </Reveal>
              ))}
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
