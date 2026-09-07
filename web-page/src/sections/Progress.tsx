import { motion } from "framer-motion";
import { Flame, Star, Trophy, Timer, AudioLines, TrendingUp } from "lucide-react";
import { Reveal, SectionHeading } from "@/components/Reveal";

const STATS = [
  { icon: Flame, label: "Practice streak", value: "14 days" },
  { icon: Star, label: "Average score", value: "87%" },
  { icon: Trophy, label: "Best performance", value: "96%" },
  { icon: Timer, label: "Practice time", value: "21h 40m" },
  { icon: AudioLines, label: "Vocal range", value: "C3 – A5" },
  { icon: TrendingUp, label: "Improvement", value: "+12% / mo" },
] as const;

export function ProgressSection() {
  return (
    <section className="section-pad">
      <div className="shell">
        <SectionHeading
          eyebrow="Personal progress"
          title={
            <>
              Your voice has a history.
              <br />
              Cadenza remembers it.
            </>
          }
          subtitle="Every take is kept, compared and turned into a curve you can watch move."
        />

        <div className="mt-14 grid gap-6 lg:grid-cols-[1.15fr_1fr]">
          <Reveal className="surface-card p-6 sm:p-8">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
              Overall score · last 6 months
            </p>
            <svg viewBox="0 0 520 220" className="mt-6 h-52 w-full sm:h-64" role="img"
              aria-label="Line chart showing overall score improving over six months">
              <defs>
                <linearGradient id="areaGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="oklch(0.58 0.25 300)" stopOpacity="0.28" />
                  <stop offset="100%" stopColor="oklch(0.58 0.25 300)" stopOpacity="0" />
                </linearGradient>
              </defs>
              {[0, 1, 2, 3].map((i) => (
                <line
                  key={i}
                  x1="0"
                  x2="520"
                  y1={30 + i * 50}
                  y2={30 + i * 50}
                  stroke="color-mix(in oklab, var(--primary) 10%, transparent)"
                />
              ))}
              <motion.path
                d="M10 180 C 80 176, 110 150, 170 148 S 250 126, 300 108 S 390 82, 440 60 S 495 44, 510 38 L510 200 L10 200 Z"
                fill="url(#areaGrad)"
                initial={{ opacity: 0 }}
                whileInView={{ opacity: 1 }}
                viewport={{ once: true }}
                transition={{ duration: 1.4, delay: 0.5 }}
              />
              <motion.path
                d="M10 180 C 80 176, 110 150, 170 148 S 250 126, 300 108 S 390 82, 440 60 S 495 44, 510 38"
                fill="none"
                stroke="var(--primary)"
                strokeWidth="4"
                strokeLinecap="round"
                initial={{ pathLength: 0 }}
                whileInView={{ pathLength: 1 }}
                viewport={{ once: true }}
                transition={{ duration: 2, ease: "easeInOut" }}
              />
              {["Mar", "Apr", "May", "Jun", "Jul", "Aug"].map((m, i) => (
                <text
                  key={m}
                  x={20 + i * 96}
                  y={214}
                  fontSize="12"
                  fill="var(--muted-foreground)"
                  fontWeight="600"
                >
                  {m}
                </text>
              ))}
            </svg>
          </Reveal>

          <div className="grid grid-cols-2 gap-4">
            {STATS.map((s, i) => (
              <Reveal key={s.label} delay={i * 0.06}>
                <motion.div
                  whileHover={{ y: -5 }}
                  transition={{ type: "spring", stiffness: 250, damping: 20 }}
                  className="surface-card h-full p-5"
                >
                  <s.icon className="h-5 w-5 text-primary" />
                  <p className="mt-4 text-lg font-extrabold tracking-tight sm:text-xl">{s.value}</p>
                  <p className="text-xs font-medium text-muted-foreground">{s.label}</p>
                </motion.div>
              </Reveal>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
