import { motion } from "framer-motion";
import { Mic, AudioWaveform, LineChart, Music4, GitCompare, MessageCircleHeart } from "lucide-react";
import { Reveal, SectionHeading } from "@/components/Reveal";

const STAGES = [
  { icon: Mic, title: "Your Voice", copy: "You sing. Cadenza records a clean take." },
  { icon: AudioWaveform, title: "Audio Analysis", copy: "The signal is cleaned and framed." },
  { icon: LineChart, title: "Pitch Extraction", copy: "A continuous pitch curve is traced." },
  { icon: Music4, title: "Musical Mapping", copy: "Frequencies become notes and timing." },
  { icon: GitCompare, title: "Performance Comparison", copy: "Your take is matched to the melody." },
  { icon: MessageCircleHeart, title: "Personalized Feedback", copy: "You get a human, useful note." },
] as const;

export function HowItWorks() {
  return (
    <section id="how-it-works" className="section-pad relative overflow-hidden">
      <div
        aria-hidden
        className="pointer-events-none absolute right-[-10%] top-1/3 h-[28rem] w-[28rem] rounded-full opacity-15 blur-[120px]"
        style={{ background: "var(--gradient-brand)" }}
      />
      <div className="shell relative">
        <SectionHeading
          eyebrow="The pipeline"
          title={<>How Cadenza listens.</>}
          subtitle="A single take travels through six stages — from raw sound to something you can act on."
        />

        <div className="mt-16">
          <svg viewBox="0 0 1200 120" className="mb-4 hidden h-24 w-full lg:block" aria-hidden>
            <defs>
              <linearGradient id="flowGrad" x1="0" x2="1">
                <stop offset="0%" stopColor="oklch(0.58 0.25 300)" />
                <stop offset="55%" stopColor="oklch(0.57 0.22 266)" />
                <stop offset="100%" stopColor="oklch(0.78 0.13 220)" />
              </linearGradient>
            </defs>
            <motion.path
              d="M0 84 C 90 84, 110 20, 200 34 S 330 100, 420 62 S 560 14, 660 54 S 820 96, 920 60 S 1100 34, 1200 44"
              fill="none"
              stroke="url(#flowGrad)"
              strokeWidth="4"
              strokeLinecap="round"
              initial={{ pathLength: 0, opacity: 0.2 }}
              whileInView={{ pathLength: 1, opacity: 1 }}
              viewport={{ once: true }}
              transition={{ duration: 2.2, ease: "easeInOut" }}
            />
          </svg>

          <ol className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {STAGES.map((s, i) => (
              <Reveal as="li" key={s.title} delay={i * 0.07}>
                <motion.div
                  whileHover={{ y: -5 }}
                  transition={{ type: "spring", stiffness: 250, damping: 20 }}
                  className="surface-card group relative h-full overflow-hidden p-6"
                >
                  <span className="absolute right-5 top-5 text-4xl font-black text-accent">
                    {String(i + 1).padStart(2, "0")}
                  </span>
                  <span className="grid h-11 w-11 place-items-center rounded-2xl bg-brand text-primary-foreground shadow-float">
                    <s.icon className="h-5 w-5" />
                  </span>
                  <h3 className="mt-5 text-lg font-bold tracking-tight">{s.title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{s.copy}</p>
                </motion.div>
              </Reveal>
            ))}
          </ol>
        </div>
      </div>
    </section>
  );
}
