import { useRef } from "react";
import { motion, useScroll, useTransform } from "framer-motion";
import { Reveal, SectionHeading } from "@/components/Reveal";

const STACK = [
  "Android",
  "Kotlin",
  "Python",
  "FastAPI",
  "Librosa",
  "Vocal Separation",
  "Audio Signal Processing",
  "Machine Learning",
] as const;

const FLOW = ["Audio", "Vocal Separation", "Analysis", "Musical Understanding", "Feedback"] as const;

export function Technology() {
  const ref = useRef<HTMLElement>(null);
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start end", "end start"],
  });

  const y1 = useTransform(scrollYProgress, [0, 1], [50, -50]);
  const y2 = useTransform(scrollYProgress, [0, 1], [100, -100]);

  return (
    <section id="technology" ref={ref} className="section-pad relative">
      <div className="shell">
        <SectionHeading
          eyebrow="Under the hood"
          title={<>Built around the science of singing.</>}
          subtitle="The engineering exists to serve one thing: feedback a singer can actually use."
        />

        <div className="mt-12 grid gap-6 lg:grid-cols-[1fr_1fr]">
          <Reveal className="h-full">
            <motion.div style={{ y: y1 }} className="surface-card p-7 h-full">
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                Stack
              </p>
              <ul className="mt-5 flex flex-wrap gap-2.5">
                {STACK.map((t, i) => (
                  <motion.li
                    key={t}
                    initial={{ opacity: 0, y: 10 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.5, delay: i * 0.05 }}
                    className="rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-foreground/80 shadow-sm"
                  >
                    {t}
                  </motion.li>
                ))}
              </ul>
            </motion.div>
          </Reveal>

          <Reveal delay={0.1} className="h-full">
            <motion.div style={{ y: y2 }} className="surface-card p-7 h-full">
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                Philosophy
              </p>
              <div className="mt-6 flex flex-wrap items-center gap-3">
                {FLOW.map((f, i) => (
                  <span key={f} className="flex items-center gap-2">
                    <span className="rounded-2xl bg-accent/70 px-4 py-2 text-sm font-bold tracking-tight">
                      {f}
                    </span>
                    {i < FLOW.length - 1 && <span className="text-primary/70">→</span>}
                  </span>
                ))}
              </div>
              <p className="mt-6 text-sm leading-relaxed text-muted-foreground">
                Recorded audio is processed through deep learning to isolate the vocals, pitch is tracked continuously, the result is
                mapped into musical space, and only then does it become language — a short, specific
                note about what to try next.
              </p>
            </motion.div>
          </Reveal>
        </div>
      </div>
    </section>
  );
}
