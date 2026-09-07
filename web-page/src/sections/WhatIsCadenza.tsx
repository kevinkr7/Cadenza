import { motion } from "framer-motion";
import { Ear, Activity, TrendingUp } from "lucide-react";
import { Reveal, SectionHeading } from "@/components/Reveal";
import { AudioVisualizer } from "@/components/AudioVisualizer";

const CONCEPTS = [
  {
    icon: Ear,
    title: "Listen",
    copy: "Cadenza captures your singing and uses deep learning to isolate your voice from background noise, creating a clean recording built for analysis.",
    visual: "waves",
  },
  {
    icon: Activity,
    title: "Understand",
    copy: "It reads pitch, timing, stability and the small details that shape a performance.",
    visual: "curve",
  },
  {
    icon: TrendingUp,
    title: "Improve",
    copy: "Analysis becomes coaching: clear feedback, practice direction and visible progress.",
    visual: "bars",
  },
] as const;

function Visual({ kind }: { kind: (typeof CONCEPTS)[number]["visual"] }) {
  if (kind === "waves") return <AudioVisualizer bars={18} className="h-24" />;
  if (kind === "curve")
    return (
      <svg viewBox="0 0 220 90" className="h-24 w-full" aria-hidden>
        <motion.path
          d="M4 66 C 40 66, 44 22, 74 24 S 118 68, 148 44 S 196 18, 216 26"
          fill="none"
          stroke="var(--primary)"
          strokeWidth="3"
          strokeLinecap="round"
          initial={{ pathLength: 0 }}
          whileInView={{ pathLength: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 1.6, ease: "easeInOut" }}
        />
        <path
          d="M4 58 C 40 58, 48 30, 74 30 S 120 60, 150 40 S 196 24, 216 30"
          fill="none"
          stroke="var(--cyan)"
          strokeWidth="2"
          strokeDasharray="5 7"
          opacity="0.7"
        />
      </svg>
    );
  return (
    <div className="flex h-24 items-end gap-2">
      {[34, 52, 46, 68, 84].map((h, i) => (
        <motion.span
          key={i}
          className="w-7 rounded-t-xl"
          style={{ background: "var(--gradient-brand)", opacity: 0.35 + i * 0.14 }}
          initial={{ height: 8 }}
          whileInView={{ height: h }}
          viewport={{ once: true }}
          transition={{ duration: 0.8, delay: i * 0.08, ease: [0.22, 1, 0.36, 1] }}
        />
      ))}
    </div>
  );
}

export function WhatIsCadenza() {
  return (
    <section id="what-is-cadenza" className="section-pad">
      <div className="shell">
        <SectionHeading
          eyebrow="What is Cadenza"
          title={<>More than a pitch detector.</>}
          subtitle="Cadenza is a personal vocal companion. It follows how you actually sing — not just whether a note was right — and turns that into practice you can feel."
        />

        <div className="mt-14 grid gap-6 md:grid-cols-3">
          {CONCEPTS.map((c, i) => (
            <Reveal key={c.title} delay={i * 0.1}>
              <motion.article
                whileHover={{ y: -6 }}
                transition={{ type: "spring", stiffness: 260, damping: 22 }}
                className="surface-card h-full p-7"
              >
                <span className="grid h-12 w-12 place-items-center rounded-2xl bg-accent/70 text-primary">
                  <c.icon className="h-6 w-6" />
                </span>
                <h3 className="heading-md mt-6">{c.title}</h3>
                <p className="mt-3 text-sm leading-relaxed text-muted-foreground">{c.copy}</p>
                <div className="mt-6">
                  <Visual kind={c.visual} />
                </div>
              </motion.article>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
