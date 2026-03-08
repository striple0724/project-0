import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { AppSessionSkeleton } from "./app/AppSessionSkeleton";
import "./index.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <AppSessionSkeleton />
  </StrictMode>
);
