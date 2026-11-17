/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["Be Vietnam Pro", "Segoe UI", "system-ui", "sans-serif"],
        display: ["Plus Jakarta Sans", "Be Vietnam Pro", "sans-serif"],
        mono: ["JetBrains Mono", "ui-monospace", "SFMono-Regular", "monospace"],
      },
      boxShadow: {
        soft: "0 1px 2px rgba(11, 17, 32, 0.04), 0 18px 40px -20px rgba(11, 17, 32, 0.16)",
        atelier: "0 20px 50px -24px rgba(11, 17, 32, 0.18), 0 2px 8px rgba(11, 17, 32, 0.04)",
      },
    },
  },
  plugins: [],
};
