import { AnimatePresence, motion } from "framer-motion";
import { useEffect, useState } from "react";
import { Logo } from "./Logo";

export function Loader() {
  const [done, setDone] = useState(false);
  useEffect(() => {
    const t = setTimeout(() => setDone(true), 1100);
    return () => clearTimeout(t);
  }, []);

  return (
    <AnimatePresence>
      {!done && (
        <motion.div
          className="fixed inset-0 z-[100] grid place-items-center bg-background"
          exit={{ opacity: 0 }}
          transition={{ duration: 0.6, ease: "easeOut" }}
          aria-hidden
        >
          <div className="relative grid place-items-center">
            {[0, 1].map((i) => (
              <motion.span
                key={i}
                className="absolute rounded-full border border-primary/30"
                style={{ width: 120, height: 120 }}
                initial={{ scale: 0.7, opacity: 0.6 }}
                animate={{ scale: 1.9, opacity: 0 }}
                transition={{ duration: 1.6, repeat: Infinity, delay: i * 0.5, ease: "easeOut" }}
              />
            ))}
            <motion.div
              initial={{ scale: 0.86, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
            >
              <Logo size={84} glow />
            </motion.div>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
