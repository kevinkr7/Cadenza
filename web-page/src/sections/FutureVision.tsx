import { motion } from "framer-motion";
import { Brain, Music, HeartHandshake, Users, Target } from "lucide-react";
import { Reveal, SectionHeading } from "@/components/Reveal";

const CARDS = [
  { icon: Brain, title: "Vocal Intelligence", copy: "Deeper analysis of singing technique — breath, onset, vibrato and control." },
  { icon: Music, title: "Song Matching", copy: "Compare a performance against the intended vocal melody and timing." },
  { icon: HeartHandshake, title: "Your Companion", copy: "A personal vocal coach that learns your goals and adapts to your progress." },
  { icon: Users, title: "Cadenza Community", copy: "Share performances, practice together and connect with other singers." },
  { icon: Target, title: "Personalized Training", copy: "Practice plans shaped around your strengths and weak spots." },
] as const;

export function FutureVision() {
  return (
    <section className="section-pad relative overflow-hidden">
      <div
        aria-hidden
        className="pointer-events-none absolute left-[-12%] top-1/4 h-[26rem] w-[26rem] rounded-full opacity-15 blur-[130px]"
        style={{ background: "var(--gradient-brand)" }}
      />
      <div className="shell relative">
        <SectionHeading
          eyebrow="Roadmap"
          title={<>Where Cadenza is going.</>}
          subtitle="The app you can download today is the foundation. These are the directions we are building towards — honestly labelled as what's next."
        />

        <div className="mt-14 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {CARDS.map((c, i) => (
            <Reveal key={c.title} delay={i * 0.07}>
              <motion.article
                whileHover={{ y: -6 }}
                transition={{ type: "spring", stiffness: 250, damping: 20 }}
                className="surface-card h-full p-6"
              >
                <span className="grid h-11 w-11 place-items-center rounded-2xl bg-brand text-primary-foreground shadow-float">
                  <c.icon className="h-5 w-5" />
                </span>
                <h3 className="mt-5 text-lg font-bold tracking-tight">{c.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{c.copy}</p>
              </motion.article>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
