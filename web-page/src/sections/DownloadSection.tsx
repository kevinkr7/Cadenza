import { motion } from "framer-motion";
import { Download, ArrowRight } from "lucide-react";
import { SITE } from "@/lib/site";
import { CTA } from "@/components/Button";
import { Logo } from "@/components/Logo";
import { Reveal } from "@/components/Reveal";
import { AudioVisualizer } from "@/components/AudioVisualizer";
import singerPhone from "@/assets/singer-phone.png";

function PhoneMockup() {
  return (
    <motion.div
      className="relative mx-auto w-[236px] sm:w-[268px]"
      animate={{ y: [0, -14, 0] }}
      transition={{ duration: 8, repeat: Infinity, ease: "easeInOut" }}
    >
      <div className="rounded-[2.6rem] border border-border bg-card p-3 shadow-lift">
        <div className="overflow-hidden rounded-[2rem] bg-[color-mix(in_oklab,var(--lavender)_88%,white)] p-4">
          <div className="mx-auto mb-4 h-1.5 w-16 rounded-full bg-accent" />
          <div className="flex items-center gap-2">
            <Logo size={26} />
            <span className="text-sm font-extrabold tracking-tight">Cadenza</span>
          </div>
          <p className="mt-4 text-[0.7rem] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
            Today&apos;s take
          </p>
          <div className="mt-2 rounded-2xl bg-card p-3 shadow-card">
            <p className="text-2xl font-extrabold tracking-tight text-gradient">90%</p>
            <p className="text-[0.65rem] font-medium text-muted-foreground">Overall performance</p>
            <AudioVisualizer bars={16} className="mt-2 h-10" />
          </div>
          <div className="mt-3 space-y-2">
            {["Pitch 92%", "Timing 88%", "Stability 91%"].map((t) => (
              <div
                key={t}
                className="flex items-center justify-between rounded-xl bg-card px-3 py-2 text-[0.7rem] font-semibold shadow-card"
              >
                <span>{t.split(" ")[0]}</span>
                <span className="text-primary">{t.split(" ")[1]}</span>
              </div>
            ))}
          </div>
          <div className="mt-4 rounded-2xl bg-brand px-3 py-2.5 text-center text-[0.72rem] font-bold text-primary-foreground">
            Record a new take
          </div>
        </div>
      </div>
    </motion.div>
  );
}

export function DownloadSection() {
  return (
    <section id="download" className="section-pad">
      <div className="shell">
        <Reveal>
          <div className="relative overflow-hidden rounded-[2.5rem] border border-border bg-[color-mix(in_oklab,var(--lavender)_92%,white)] px-6 py-14 sm:px-10 lg:px-16">
            <div
              aria-hidden
              className="pointer-events-none absolute -right-16 -top-16 h-72 w-72 rounded-full opacity-30 blur-[90px]"
              style={{ background: "var(--gradient-brand)" }}
            />
            <div className="relative grid items-center gap-12 lg:grid-cols-[1.05fr_0.95fr]">
              <div>
                <Logo size={52} animated glow />
                <h2 className="heading-lg mt-6 text-balance">
                  Ready to hear yourself <span className="text-gradient">differently?</span>
                </h2>
                <p className="mt-5 max-w-lg text-pretty text-base leading-relaxed text-muted-foreground sm:text-lg">
                  Take Cadenza with you and start building your voice.
                </p>
                <div className="mt-9 flex flex-wrap gap-3">
                  <CTA href={SITE.downloadUrl} external size="lg">
                    <Download className="h-4 w-4 transition-transform group-hover:translate-y-0.5" />
                    Download for Android
                  </CTA>
                  <CTA href={SITE.joinUrl} external variant="outline" size="lg">
                    Join the Cadenza journey
                    <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
                  </CTA>
                </div>
              </div>
              <div className="relative flex items-end justify-center">
                <img
                  src={singerPhone}
                  alt="Person sitting cross-legged singing along with their phone"
                  loading="lazy"
                  width={1024}
                  height={1280}
                  className="w-[62%] max-w-[19rem] object-contain drop-shadow-[0_30px_50px_rgba(80,40,160,0.25)]"
                />
                <div className="-ml-8 sm:-ml-10">
                  <PhoneMockup />
                </div>
              </div>
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
