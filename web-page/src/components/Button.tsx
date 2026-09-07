import type { ReactNode } from "react";

type Variant = "primary" | "ghost" | "outline";

const base =
  "group inline-flex items-center justify-center gap-2 rounded-full text-sm font-semibold transition-all duration-300 will-change-transform active:scale-[0.98] disabled:opacity-50";

const sizes = {
  md: "px-5 py-2.5",
  lg: "px-7 py-3.5 text-[0.95rem]",
} as const;

const variants: Record<Variant, string> = {
  primary:
    "bg-brand text-primary-foreground shadow-float hover:-translate-y-0.5 hover:shadow-lift",
  outline:
    "border border-border bg-card/80 text-foreground hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-card",
  ghost: "text-foreground/80 hover:text-foreground",
};

export function CTA({
  children,
  href,
  variant = "primary",
  size = "md",
  className = "",
  external = false,
  onClick,
  ariaLabel,
}: {
  children: ReactNode;
  href?: string;
  variant?: Variant;
  size?: keyof typeof sizes;
  className?: string;
  external?: boolean;
  onClick?: () => void;
  ariaLabel?: string;
}) {
  const cls = `${base} ${sizes[size]} ${variants[variant]} ${className}`;
  if (href) {
    return (
      <a
        href={href}
        className={cls}
        aria-label={ariaLabel ?? undefined}
        {...(external ? { target: "_blank", rel: "noreferrer noopener" } : {})}
      >
        {children}
      </a>
    );
  }
  return (
    <button type="button" onClick={onClick} className={cls} aria-label={ariaLabel ?? undefined}>
      {children}
    </button>
  );
}
