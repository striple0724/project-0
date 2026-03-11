import { create } from "zustand";
import { persist } from "zustand/middleware";

export type ThemeMode = "light" | "bright-navy" | "dark-navy";

interface ThemeState {
  mode: ThemeMode;
  setMode: (mode: ThemeMode) => void;
  toggleNext: () => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      mode: "light",
      setMode: (mode) => {
        set({ mode });
        applyTheme(mode);
      },
      toggleNext: () =>
        set((state) => {
          let next: ThemeMode;
          if (state.mode === "light") next = "bright-navy";
          else if (state.mode === "bright-navy") next = "dark-navy";
          else next = "light";
          
          applyTheme(next);
          return { mode: next };
        }),
    }),
    {
      name: "tax-workbench-theme-v2",
      onRehydrateStorage: () => (state) => {
        applyTheme(state?.mode || "light");
      },
    }
  )
);

function applyTheme(mode: ThemeMode) {
  if (typeof document === "undefined") return;
  const root = document.documentElement;
  root.classList.remove("theme-bright-navy", "theme-dark-navy", "theme-light");
  root.classList.add(`theme-${mode}`);
  
  // Also sync dark class for Tailwind's dark: prefix
  if (mode === "light") {
    root.classList.remove("dark");
  } else {
    root.classList.add("dark");
  }
}
