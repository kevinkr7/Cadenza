import { motion } from "framer-motion";
import { Heart, Sparkles } from "lucide-react";
import { Reveal, SectionHeading } from "@/components/Reveal";
import singerMan from "@/assets/singer-man.png";

const MESSAGES = [
  "That note was much more stable this time.",
  "You're getting stronger in your upper range.",
  "Let's work on that transition again.",
] as const;

export function AICompanion() {
  return (
    <section className="section-pad relative overflow-hidden">
      <div className="shell">
        <SectionHeading
          eyebrow="Product vision"
          title={<>A companion that hears you, not a chatbot.</>}
          subtitle="Cadenza is growing towards a warm, encouraging vocal companion — one that remembers your goals and speaks like a teacher who actually listened. Parts of this are still ahead of us."
        />

        <div className="mt-16 grid items-center gap-10 lg:grid-cols-[0.9fr_1.1fr]">
          <Reveal className="relative mx-auto w-full max-w-sm">
            <div className="relative aspect-square">
              {[0, 1, 2].map((i) => (
                <motion.span
                  key={i}
                  className="absolute inset-0 rounded-full border border-primary/20"
                  animate={{ scale: [0.75, 1.15], opacity: [0.5, 0] }}
                  transition={{ duration: 3.4, repeat: Infinity, delay: i * 1.1, ease: "easeOut" }}
                />
              ))}
              <motion.div
                className="absolute inset-[14%] overflow-hidden rounded-[42%] shadow-lift"
                style={{ background: "var(--gradient-brand)" }}
                animate={{
                  borderRadius: ["42% 58% 55% 45%", "56% 44% 40% 60%", "42% 58% 55% 45%"],
                  y: [0, -12, 0],
                }}
                transition={{ duration: 9, repeat: Infinity, ease: "easeInOut" }}
              >
                <img
                  src={singerMan}
                  alt="Singer with headphones at a studio microphone"
                  loading="lazy"
                  width={1024}
                  height={1280}
                  className="absolute inset-0 h-full w-full scale-110 object-cover object-top mix-blend-luminosity opacity-90"
                />
              </motion.div>
              <span className="absolute right-[8%] top-[10%] grid h-12 w-12 place-items-center rounded-2xl bg-card text-primary shadow-card">
                <Sparkles className="h-5 w-5" />
              </span>
            </div>
            <p className="mt-6 text-center text-sm text-muted-foreground">
              Your companion — expressive, customizable, always on your side.
            </p>
          </Reveal>

          <ul className="space-y-4">
            {MESSAGES.map((m, i) => (
              <Reveal as="li" key={m} delay={i * 0.12}>
                <motion.div
                  whileHover={{ y: -4 }}
                  className={`surface-card flex items-start gap-4 p-5 sm:p-6 ${
                    i % 2 ? "lg:ml-12" : ""
                  }`}
                >
                  <span className="grid h-10 w-10 shrink-0 place-items-center rounded-2xl bg-accent/70 text-primary">
                    <Heart className="h-5 w-5" />
                  </span>
                  <p className="min-w-0 text-pretty text-base font-medium leading-relaxed sm:text-lg">
                    “{m}”
                  </p>
                </motion.div>
              </Reveal>
            ))}
            <Reveal as="li" delay={0.4}>
              <p className="pt-2 text-sm text-muted-foreground lg:ml-12">
                Encouragement first, corrections second — the way good coaching actually works.
              </p>
            </Reveal>
          </ul>
        </div>
      </div>
    </section>
  );
}
