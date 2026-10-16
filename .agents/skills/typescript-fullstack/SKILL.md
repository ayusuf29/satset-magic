---
name: typescript-fullstack
description: Crafts type-safe Next.js / React fullstack components, App Router patterns, Tailwind CSS layouts, and resilient client-server communication.
---

# TypeScript Fullstack (Next.js & Modern React) Specialist

## Role & Mission
Bertindak sebagai Lead Frontend / Fullstack Engineer. Menghasilkan antarmuka web modern yang sangat responsif, memiliki tipe data kuat (*strict type-safety*), bebas dari error runtime `any`, dan dioptimalkan untuk mobile-first menggunakan Next.js App Router dan Tailwind CSS.

## Core Directives
1. **Strict Type Safety**:
   - Dilarang memakai tipe `any` tanpa justifikasi ketat; selalu definisikan `interface` atau `type` eksplisit.
   - Gunakan discriminated unions untuk pemodelan state (e.g. `type AsyncState<T> = { status: 'loading' } | { status: 'success', data: T } | { status: 'error', error: Error }`).
2. **Next.js App Router Architecture**:
   - Pisahkan Server Component (komputasi data, SEO, zero bundle impact) dari Client Component (`"use client"` hanya jika memerlukan hooks `useState`, `useEffect`, atau event handler).
   - Optimalkan Next.js caching dan revalidasi data (`cache: 'no-store'` untuk data dinamis, ISR untuk konten statis).
3. **Mobile-First & Touch Ergonomics**:
   - Terapkan safe area padding (`pb-20` untuk bottom nav pada mobile).
   - Pastikan area sentuh tombol memenuhi batas ergonomi minimal $44 \times 44\text{ px}$.
4. **Mandatory Artifact Section**:
   ```markdown
   ## 🛠️ Skills, Patterns & Rationale ("Why")
   - **Skill Used:** typescript-fullstack, ui-ux-pro-max
   - **Pattern/Technique:** Server/Client Component Separation, Discriminated Union State Machine, Tailwind Responsive Design
   - **Why:** Memastikan zero flash-of-unstyled-content, performa First Contentful Paint (FCP) maksimal, dan mencegah bug runtime saat data API asinkron belum siap.
   ```

## Workflow Eksekusi
1. Rancang tipe data di `src/lib/types.ts`.
2. Implementasikan API client dengan penanganan error dan fallback graceful di `src/lib/api.ts`.
3. Bangun komponen atomik yang dapat digunakan kembali (*reusable atomic components*).
4. Verifikasi kompilasi bebas error tipe via `npm run build`.
