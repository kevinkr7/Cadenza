import { motion } from "framer-motion";
import { Mic2, ScanLine, Lightbulb, ArrowUpRight, Sparkle } from "lucide-react";
import { Reveal, SectionHeading } from "@/components/Reveal";

const STEPS = [
  { icon: Mic2, title: "Practice", copy: "Warm up and record a take whenever the moment hits." },
  { icon: ScanLine, title: "Analyze", copy: "Pitch, timing and stability are measured instantly." },
  { icon: Lightbulb, title: "Understand", copy: "See exactly where the note drifted, and why." },
  { icon: ArrowUpRight, title: "Improve", copy: "Repeat the phrase with targeted guidance." },
  { icon: Sparkle, title: "Perform", copy: "Walk into the room already sure of your voice." },
] as const;

export function PracticeToPerformance() {
  return (
    <section className="section-pad">
      <div className="shell">
        <SectionHeading
          eyebrow="The loop"
          title={<>From practice to performance.</>}
          subtitle="Five steps you repeat until the change stops feeling like effort."
        />

        <ol className="mt-14 flex snap-x snap-mandatory gap-5 overflow-x-auto pb-4 lg:grid lg:grid-cols-5 lg:overflow-visible lg:pb-0">
          {STEPS.map((s, i) => (
            <Reveal
              as="li"
              key={s.title}
              delay={i * 0.08}
              className="min-w-[74%] snap-start sm:min-w-[42%] lg:min-w-0"
            >
              <motion.div
                whileHover={{ y: -6 }}
                transition={{ type: "spring", stiffness: 250, damping: 20 }}
                className="surface-card relative h-full p-6"
              >
                <span className="text-xs font-bold uppercase tracking-[0.2em] text-primary">
                  Step {i + 1}
                </span>
                <motion.span
                  className="mt-4 grid h-12 w-12 place-items-center rounded-2xl bg-accent/70 text-primary"
                  whileHover={{ rotate: -8, scale: 1.06 }}
                >
                  <s.icon className="h-6 w-6" />
                </motion.span>
                <h3 className="mt-5 text-lg font-bold tracking-tight">{s.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{s.copy}</p>
              </motion.div>
            </Reveal>
          ))}
        </ol>
      </div>
    </section>
  );
}
