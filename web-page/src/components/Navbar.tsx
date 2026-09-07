import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Download, Menu, X } from "lucide-react";
import { NAV_LINKS, SITE } from "@/lib/site";
import { Wordmark } from "./Logo";
import { CTA } from "./Button";

export function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(() => {
    document.body.style.overflow = open ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [open]);

  return (
    <header className="fixed inset-x-0 top-0 z-50">
      <div
        className={`transition-all duration-500 ${
          scrolled
            ? "border-b border-border/70 bg-background/80 backdrop-blur-xl shadow-[0_10px_30px_-24px_rgba(60,40,140,0.5)]"
            : "border-b border-transparent bg-transparent"
        }`}
      >
        <nav
          aria-label="Main"
          className="shell grid grid-cols-[minmax(0,1fr)_auto] items-center gap-4 py-3.5 lg:flex lg:justify-between"
        >
          <a href="#home" className="min-w-0" aria-label="Cadenza home">
            <Wordmark size={34} />
          </a>

          <ul className="hidden items-center gap-1 lg:flex">
            {NAV_LINKS.map((l) => (
              <li key={l.href}>
                <a
                  href={l.href}
                  className="rounded-full px-3.5 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-accent/60 hover:text-foreground"
                >
                  {l.label}
                </a>
              </li>
            ))}
          </ul>

          <div className="hidden lg:block">
            <CTA href={SITE.downloadUrl} external ariaLabel="Download the Cadenza app">
              <Download className="h-4 w-4 transition-transform group-hover:translate-y-0.5" />
              Download App
            </CTA>
          </div>

          <button
            type="button"
            onClick={() => setOpen((v) => !v)}
            aria-label={open ? "Close menu" : "Open menu"}
            aria-expanded={open}
            className="grid h-11 w-11 shrink-0 place-items-center rounded-full border border-border bg-card/80 lg:hidden"
          >
            {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </nav>
      </div>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: -12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -12 }}
            transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
            className="shell lg:hidden"
          >
            <div className="surface-card mt-3 flex flex-col gap-1 p-4">
              {NAV_LINKS.map((l) => (
                <a
                  key={l.href}
                  href={l.href}
                  onClick={() => setOpen(false)}
                  className="rounded-2xl px-4 py-3 text-base font-semibold text-foreground/85 transition-colors hover:bg-accent/60"
                >
                  {l.label}
                </a>
              ))}
              <CTA
                href={SITE.downloadUrl}
                external
                size="lg"
                className="mt-2 w-full"
                ariaLabel="Download the Cadenza app"
              >
                <Download className="h-4 w-4" />
                Download App
              </CTA>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
}
