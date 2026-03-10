import { create } from "zustand";
import { persist } from "zustand/middleware";

export type ThemeMode = "deep" | "bright";

interface ThemeState {
  mode: ThemeMode;
  setMode: (mode: ThemeMode) => void;
  toggleMode: () => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      mode: "bright",
      setMode: (mode) => {
        set({ mode });
        applyTheme(mode);
      },
      toggleMode: () =>
        set((state) => {
          const next = state.mode === "deep" ? "bright" : "deep";
          applyTheme(next);
          return { mode: next };
        }),
    }),
    {
      name: "tax-workbench-theme",
      onRehydrateStorage: () => (state) => {
        // Use bright as default if no state is persisted
        applyTheme(state?.mode || "bright");
      },
    }
  )
);

function applyTheme(mode: ThemeMode) {
  if (typeof document === "undefined") return;
  const root = document.documentElement;
  if (mode === "bright") {
    root.classList.add("theme-bright");
    root.classList.remove("theme-deep");
  } else {
    root.classList.add("theme-deep");
    root.classList.remove("theme-bright");
  }
}
