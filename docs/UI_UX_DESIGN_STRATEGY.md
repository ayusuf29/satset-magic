# UI/UX DESIGN STRATEGY: FIGMA IMPORT VS PURE BLANK

## Mode A: Client Has Existing Design (Figma / Adobe XD)
1. Agent meminta link Figma atau export file tokens JSON / screens.
2. Menggunakan Figma REST API atau Gemini Vision untuk mengekstrak:
   - Color variables, Typography styles, Spacing rules.
   - Component hierarchy & layout auto-layouts.
3. Menghasilkan file `design-tokens.json` yang di-inject langsung ke styling Tailwind/CSS.

## Mode B: Pure Blank (No Existing Design)
1. UI/UX Designer Agent membuat design token baseline berbasis tema industri target.
2. Menggunakan komponen headless teruji (misal: Radix UI / Shadcn / Tailwind UI).
3. Menghasilkan wireframe spesifikasi dalam markdown sebelum coding dimulai.
