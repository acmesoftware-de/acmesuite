import type { Deal, PipelineStage } from './crmApi'

// Pure pipeline model: stage order, stage-derived probability, value formatting and the
// funnel projection. No colors here — stage/owner colors are theme-owned and applied in
// themes/base/components.css via the data-stage / data-owner attributes the views emit.

export interface StageMeta {
  id: PipelineStage
  /** Uppercase board/column/select label. */
  label: string
  /** Win probability derived from the stage (per the design spec). */
  probability: number
}

export const STAGES: StageMeta[] = [
  { id: 'NEU', label: 'NEU', probability: 15 },
  { id: 'QUALIFIZIERT', label: 'QUALIFIZIERT', probability: 35 },
  { id: 'ANGEBOT', label: 'ANGEBOT', probability: 60 },
  { id: 'VERHANDLUNG', label: 'VERHANDLUNG', probability: 80 },
  { id: 'GEWONNEN', label: 'GEWONNEN', probability: 100 },
]

const PROB_BY_STAGE: Record<PipelineStage, number> = Object.fromEntries(
  STAGES.map((s) => [s.id, s.probability]),
) as Record<PipelineStage, number>

export function probabilityFor(stage: PipelineStage): number {
  return PROB_BY_STAGE[stage] ?? 0
}

export function stageLabel(stage: PipelineStage): string {
  return STAGES.find((s) => s.id === stage)?.label ?? stage
}

/** Numeric amount of a Money value (0 when unset). */
export function amountOf(deal: Deal): number {
  return deal.value?.amount ?? 0
}

/** Owner avatar initials (drives the data-owner color; "—" when unassigned). */
export function ownerInitials(deal: Deal): string {
  return deal.owner?.initials ?? '—'
}

/** Compact euro, e.g. 84000 → "€84k", 23500 → "€23,5k" (de-DE). */
export function fmtK(amount: number): string {
  if (amount >= 1000) {
    return '€' + (amount / 1000).toLocaleString('de-DE', { maximumFractionDigits: 1 }) + 'k'
  }
  return '€' + amount.toLocaleString('de-DE')
}

/** Grouped integer for the editable value cell, e.g. 84000 → "84.000". */
export function fmtFull(amount: number): string {
  return Math.round(amount).toLocaleString('de-DE')
}

/** Larger totals: ≥ 1 Mio → "€2,40 Mio", otherwise compact "k". */
export function fmtTotal(amount: number): string {
  if (amount >= 1e6) {
    return '€' + (amount / 1e6).toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' Mio'
  }
  return fmtK(amount)
}

export interface StageAggregate {
  stage: StageMeta
  deals: Deal[]
  count: number
  sum: number
}

/** Group deals by stage in canonical stage order. */
export function byStage(deals: Deal[]): StageAggregate[] {
  return STAGES.map((stage) => {
    const inStage = deals.filter((d) => d.stage === stage.id)
    return {
      stage,
      deals: inStage,
      count: inStage.length,
      sum: inStage.reduce((acc, d) => acc + amountOf(d), 0),
    }
  })
}

export interface FunnelRow extends StageAggregate {
  /** Bar width as a percentage of the busiest stage (0–100). */
  widthPct: number
  /** Share of all deals in this stage, e.g. "30%". */
  sharePct: string
  /** Conversion vs. the previous stage, or "—" for the first. */
  conversion: string
}

/** Funnel projection: bar width ∝ count, share of total, conversion to previous stage. */
export function funnelRows(deals: Deal[]): FunnelRow[] {
  const groups = byStage(deals)
  const total = deals.length || 1
  const maxCount = Math.max(1, ...groups.map((g) => g.count))
  return groups.map((g, i) => ({
    ...g,
    widthPct: Math.round(Math.max(24, (g.count / maxCount) * 100)),
    sharePct: Math.round((g.count / total) * 100) + '%',
    conversion:
      i === 0
        ? '—'
        : groups[i - 1].count
          ? Math.round((g.count / groups[i - 1].count) * 100) + '%'
          : '0%',
  }))
}
