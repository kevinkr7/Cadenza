import { Suspense, lazy, useEffect, useState, useRef } from "react";
import { motion, useScroll, useTransform } from "framer-motion";
import { ArrowRight, Download, Sparkles } from "lucide-react";
import { SITE } from "@/lib/site";
import { CTA } from "./Button";
import { Logo } from "./Logo";
import { HeroFallback } from "./AudioVisualizer";

const HeroScene = lazy(() => import("./three/HeroScene"));

function useWebGL() {
  const [ok, setOk] = useState<boolean | null>(null);
  useEffect(() => {
    try {
      const canvas = document.createElement("canvas");
      setOk(Boolean(canvas.getContext("webgl2") ?? canvas.getContext("webgl")));
    } catch {
      setOk(false);
    }
  }, []);
  return ok;
}

function useCompact() {
  const [compact, setCompact] = useState(false);
  useEffect(() => {
    const mq = window.matchMedia("(max-width: 1023px)");
    const update = () => setCompact(mq.matches);
    update();
    mq.addEventListener("change", update);
    return () => mq.removeEventListener("change", update);
  }, []);
  return compact;
}

function StatChip({ label, value, className }: { label: string; value: string; className: string }) {
  return (
    <motion.div
      className={`surface-card pointer-events-none absolute hidden px-4 py-3 md:block ${className}`}
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: [0, -10, 0] }}
      transition={{
        opacity: { duration: 0.8, delay: 0.6 },
        y: { duration: 7, repeat: Infinity, ease: "easeInOut" },
      }}
    >
      <p className="text-[0.65rem] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
        {label}
      </p>
      <p className="text-xl font-extrabold tracking-tight text-gradient">{value}</p>
    </motion.div>
  );
}

export function Hero() {
  const webgl = useWebGL();
  const compact = useCompact();

  const ref = useRef<HTMLElement>(null);
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start start", "end start"],
  });

  const bgY = useTransform(scrollYProgress, [0, 1], ["0%", "40%"]);
  const textY = useTransform(scrollYProgress, [0, 1], ["0%", "20%"]);
  const visualY = useTransform(scrollYProgress, [0, 1], ["0%", "10%"]);

  return (
    <section id="home" ref={ref} className="relative overflow-hidden pt-28 sm:pt-32 lg:pt-36">
      <motion.div
        aria-hidden
        style={{ y: bgY, background: "var(--gradient-brand)" }}
        className="pointer-events-none absolute -top-40 left-1/2 h-[36rem] w-[36rem] -translate-x-1/2 rounded-full opacity-25 blur-[110px]"
      />
      <div
        aria-hidden
        className="pointer-events-none absolute inset-x-0 bottom-0 h-64 z-10"
        style={{ background: "linear-gradient(180deg, transparent, var(--background))" }}
      />

      <div className="shell relative grid items-center gap-10 lg:grid-cols-[1.05fr_1fr] lg:gap-6 z-20">
        {/* Mobile-first: 3D visual sits above the headline on small screens */}
        <motion.div style={{ y: visualY }} className="relative order-2 h-[300px] w-full sm:h-[380px] lg:order-2 lg:h-[620px]">
          <StatChip label="Pitch accuracy" value="92%" className="right-0 top-8 z-10" />
          <StatChip label="Streak" value="14 days" className="bottom-16 left-0 z-10" />
          {webgl === false ? (
            <HeroFallback />
          ) : webgl ? (
            <Suspense fallback={<HeroFallback />}>
              <HeroScene compact={compact} />
            </Suspense>
          ) : (
            <HeroFallback />
          )}
        </motion.div>

        <motion.div style={{ y: textY }} className="order-1 lg:order-1 scale-85 origin-center lg:origin-left -mt-8 lg:-mt-16">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
            className="flex items-center gap-3"
          >
            <Logo size={30} animated glow />
            <span className="inline-flex items-center gap-2 rounded-full border border-border bg-card/70 px-3.5 py-1.5 text-xs font-semibold tracking-wide text-muted-foreground">
              <Sparkles className="h-3.5 w-3.5 text-primary" />
              Personal vocal coaching
            </span>
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 26 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.08, ease: [0.22, 1, 0.36, 1] }}
            className="heading-xl mt-6 text-balance"
          >
            Meet the voice
            <br />
            you&apos;ve <span className="text-gradient">always had.</span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 22 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.18 }}
            className="mt-6 max-w-xl text-pretty text-base leading-relaxed text-muted-foreground sm:text-lg"
          >
            Cadenza listens to the way you sing, isolates your vocals, understands your performance, and helps you become
            a better singer — one note at a time.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 22 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.26 }}
            className="mt-9 flex flex-wrap items-center gap-3"
          >
            <CTA href={SITE.downloadUrl} external size="lg">
              <Download className="h-4 w-4 transition-transform group-hover:translate-y-0.5" />
              Download Cadenza
            </CTA>
            <CTA href="#what-is-cadenza" variant="outline" size="lg">
              Explore Cadenza
              <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
            </CTA>
          </motion.div>

          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.8, delay: 0.4 }}
            className="mt-8 text-sm text-muted-foreground"
          >
            {SITE.tagline}
          </motion.p>
        </motion.div>
      </div>
    </section>
  );
}
