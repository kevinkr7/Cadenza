import { Reveal } from "@/components/Reveal";
import { AudioVisualizer } from "@/components/AudioVisualizer";
import singerWoman from "@/assets/singer-woman.png";

const POINTS = [
  { title: "Accessible", copy: "Meaningful vocal feedback outside of one-on-one lessons." },
  { title: "Personal", copy: "Guidance shaped by how you sing, not a generic curriculum." },
  { title: "Measurable", copy: "Progress you can see across weeks, not just feel." },
  { title: "Musical", copy: "Analysis that speaks in notes, phrases and timing." },
] as const;

export function About() {
  return (
    <section id="about" className="section-pad">
      <div className="shell grid gap-12 lg:grid-cols-[1fr_0.85fr] lg:gap-16">
        <Reveal>
          <span className="mb-4 inline-flex items-center gap-2 rounded-full border border-border bg-card/70 px-3.5 py-1.5 text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
            <span className="h-1.5 w-1.5 rounded-full bg-brand" />
            About Cadenza
          </span>
          <h2 className="heading-lg text-balance">
            Good coaching shouldn&apos;t depend on who you can afford to meet.
          </h2>
          <div className="mt-7 space-y-5 text-pretty text-base leading-relaxed text-muted-foreground sm:text-lg">
            <p>
              Cadenza is being developed as a vocal training platform that makes useful, specific
              feedback available to anyone who sings — at home, between lessons, or long before a
              first lesson ever happens.
            </p>
            <p>
              The goal isn&apos;t to replace a teacher. It&apos;s to give singers a way to hear
              themselves clearly, understand what changed, and keep a record of a voice as it grows.
            </p>
          </div>
          <AudioVisualizer bars={30} className="mt-10 h-14 justify-start" />
        </Reveal>

        <Reveal delay={0.12} className="grid content-start gap-4">
          <div className="relative isolate mx-auto w-full max-w-sm">
            <div
              aria-hidden
              className="absolute inset-x-6 bottom-6 top-10 -z-10 rounded-[42%_58%_50%_50%] opacity-30 blur-[60px]"
              style={{ background: "var(--gradient-brand)" }}
            />
            <img
              src={singerWoman}
              alt="Singer performing into a handheld microphone"
              loading="lazy"
              width={1024}
              height={1280}
              className="mx-auto w-full max-w-[19rem] object-contain drop-shadow-[0_30px_50px_rgba(80,40,160,0.28)]"
            />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            {POINTS.map((p) => (
              <div key={p.title} className="surface-card p-6">
                <h3 className="text-base font-bold tracking-tight">{p.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{p.copy}</p>
              </div>
            ))}
          </div>
        </Reveal>
      </div>
    </section>
  );
}
