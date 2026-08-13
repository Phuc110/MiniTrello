# Design notes

A quick rationale for the visual choices baked into `tailwind.config.ts`,
since "why this and not generic Tailwind blue" is worth writing down once
rather than re-litigating in review.

**Brief (self-authored, since this is an internal tool, not a client
brief):** an "engineering command center" feel for a PM tool built by and
for small software teams — precise, unfussy, technical — rather than a
generic consumer-SaaS look.

- **Palette**: `ink` (near-black navy, #14171F) + `paper` (soft neutral
  white, #F7F7F5) as the base, with a single distinctive accent — cobalt
  `#2F5FF6` — rather than the generic indigo/violet most component
  libraries default to. Priority colors (`priority.low/medium/high/urgent`)
  are semantic, not decorative, and are the ONLY place color is used to
  convey status.
- **Type**: Space Grotesk for headings (geometric, slightly technical),
  Inter for body/UI text (proven readability at small sizes), JetBrains
  Mono for anything identifier-like — slugs, dates, role badges, task
  counts. Three families doing three distinct jobs, not decoration.
- **Signature element**: the colored "spine" on the left edge of a
  `TaskCard` (see `TaskCard.tsx`) encodes priority at a glance across an
  entire board without needing to read a badge on every card — chosen
  deliberately over a corner badge or colored background, which would be
  louder and harder to scan in aggregate.

None of this is final — swap tokens in `tailwind.config.ts` freely: every
component consumes them via Tailwind classes, not hardcoded hex values.
