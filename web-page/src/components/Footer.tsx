import type { SVGProps } from "react";

const Github = (p: SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden {...p}>
    <path d="M12 2a10 10 0 0 0-3.16 19.49c.5.09.68-.22.68-.48l-.01-1.7c-2.78.6-3.37-1.34-3.37-1.34-.45-1.16-1.11-1.47-1.11-1.47-.91-.62.07-.6.07-.6 1 .07 1.53 1.03 1.53 1.03.9 1.53 2.34 1.09 2.91.83.09-.65.35-1.09.63-1.34-2.22-.25-4.56-1.11-4.56-4.95 0-1.09.39-1.98 1.03-2.68-.1-.25-.45-1.27.1-2.64 0 0 .84-.27 2.75 1.02a9.5 9.5 0 0 1 5 0c1.91-1.29 2.75-1.02 2.75-1.02.55 1.37.2 2.39.1 2.64.64.7 1.03 1.59 1.03 2.68 0 3.85-2.34 4.7-4.57 4.95.36.31.68.92.68 1.85l-.01 2.75c0 .27.18.58.69.48A10 10 0 0 0 12 2Z" />
  </svg>
);
const Linkedin = (p: SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden {...p}>
    <path d="M4.98 3.5A2.5 2.5 0 1 1 2.5 6 2.5 2.5 0 0 1 4.98 3.5ZM3 8.98h4v12H3v-12Zm6.5 0h3.83v1.64h.05a4.2 4.2 0 0 1 3.78-2.08c4.04 0 4.79 2.66 4.79 6.12v6.32h-4v-5.6c0-1.34-.02-3.06-1.87-3.06-1.87 0-2.16 1.46-2.16 2.96v5.7h-4v-12Z" />
  </svg>
);
const Instagram = (p: SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden {...p}>
    <rect x="3" y="3" width="18" height="18" rx="5" />
    <circle cx="12" cy="12" r="4" />
    <circle cx="17.2" cy="6.8" r="1.1" fill="currentColor" stroke="none" />
  </svg>
);
const Youtube = (p: SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden {...p}>
    <path d="M21.6 7.2a2.5 2.5 0 0 0-1.76-1.77C18.25 5 12 5 12 5s-6.25 0-7.84.43A2.5 2.5 0 0 0 2.4 7.2 26 26 0 0 0 2 12a26 26 0 0 0 .4 4.8 2.5 2.5 0 0 0 1.76 1.77C5.75 19 12 19 12 19s6.25 0 7.84-.43a2.5 2.5 0 0 0 1.76-1.77A26 26 0 0 0 22 12a26 26 0 0 0-.4-4.8ZM10 15.2V8.8L15.5 12 10 15.2Z" />
  </svg>
);

import { SITE } from "@/lib/site";
import { Logo } from "./Logo";

const LINKS = [
  { label: "Home", href: "#home" },
  { label: "Features", href: "#features" },
  { label: "Technology", href: "#technology" },
  { label: "About", href: "#about" },
  { label: "Download", href: "#download" },
  { label: "Contact", href: "mailto:hello@cadenza.app" },
] as const;

const SOCIAL = [
  { icon: Github, href: SITE.social.github, label: "GitHub" },
  { icon: Linkedin, href: SITE.social.linkedin, label: "LinkedIn" },
  { icon: Instagram, href: SITE.social.instagram, label: "Instagram" },
  { icon: Youtube, href: SITE.social.youtube, label: "YouTube" },
] as const;

export function Footer() {
  return (
    <footer className="relative overflow-hidden border-t border-border pt-16">
      <svg
        aria-hidden
        viewBox="0 0 1200 120"
        className="pointer-events-none absolute inset-x-0 top-0 h-24 w-full opacity-20"
        preserveAspectRatio="none"
      >
        <path
          d="M0 70 C 80 70, 100 24, 180 36 S 320 96, 400 60 S 540 16, 620 52 S 780 98, 860 62 S 1060 30, 1200 46"
          fill="none"
          stroke="var(--primary)"
          strokeWidth="3"
          strokeLinecap="round"
        />
      </svg>

      <div className="shell relative pb-10">
        <div className="grid gap-10 lg:grid-cols-[1.2fr_1fr_auto]">
          <div>
            <span className="flex items-center gap-3">
              <Logo size={44} />
              <span className="text-xl font-extrabold tracking-[-0.03em]">Cadenza</span>
            </span>
            <p className="mt-4 max-w-xs text-pretty text-sm leading-relaxed text-muted-foreground">
              Your voice. Your journey.
            </p>
          </div>

          <nav aria-label="Footer">
            <ul className="grid grid-cols-2 gap-3 text-sm font-medium">
              {LINKS.map((l) => (
                <li key={l.label}>
                  <a
                    href={l.href}
                    className="text-muted-foreground transition-colors hover:text-foreground"
                  >
                    {l.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>

          <ul className="flex gap-2.5">
            {SOCIAL.map((s) => (
              <li key={s.label}>
                <a
                  href={s.href}
                  target="_blank"
                  rel="noreferrer noopener"
                  aria-label={s.label}
                  className="grid h-11 w-11 place-items-center rounded-full border border-border bg-card text-muted-foreground transition-all hover:-translate-y-0.5 hover:text-primary hover:shadow-card"
                >
                  <s.icon className="h-4.5 w-4.5" />
                </a>
              </li>
            ))}
          </ul>
        </div>

        <div className="mt-12 flex flex-col gap-3 border-t border-border pt-6 text-xs text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
          <p>© 2026 Cadenza</p>
          <div className="flex gap-5">
            <a href="#about" className="transition-colors hover:text-foreground">
              Privacy Policy
            </a>
            <a href="#about" className="transition-colors hover:text-foreground">
              Terms
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
