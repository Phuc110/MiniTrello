import type { Config } from "tailwindcss";

/**
 * Design tokens for Mini Trello Enterprise — chosen deliberately for an
 * "engineering command center" feel (precise, technical, unfussy) rather
 * than a generic SaaS-blue theme. See README-design-notes.md for the
 * full rationale.
 */
export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: {
          DEFAULT: "#14171F",
          50: "#F5F6F8",
          100: "#E7E9EE",
          200: "#C7CCD6",
          300: "#9AA2B2",
          400: "#6B7385",
          500: "#4A5165",
          600: "#363C4C",
          700: "#262A36",
          800: "#1B1E27",
          900: "#14171F",
        },
        paper: "#F7F7F5",
        accent: {
          DEFAULT: "#2F5FF6",
          50: "#EEF2FF",
          100: "#DCE4FE",
          400: "#5B7EF8",
          500: "#2F5FF6",
          600: "#1E46D6",
          700: "#1735A8",
        },
        priority: {
          low: "#6B7385",
          medium: "#2F5FF6",
          high: "#F5A524",
          urgent: "#EF4444",
        },
      },
      fontFamily: {
        display: ["'Space Grotesk'", "sans-serif"],
        body: ["'Inter'", "sans-serif"],
        mono: ["'JetBrains Mono'", "monospace"],
      },
      borderRadius: {
        card: "10px",
      },
    },
  },
  plugins: [],
} satisfies Config;
