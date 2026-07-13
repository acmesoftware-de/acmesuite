import { useCallback, useEffect, useRef, useState } from 'react'
import { streamAssist, type AssistContext } from './assistApi'

export interface AssistTurn {
  question: string
  answer: string
  tools: string[]
  sources: string[]
  streaming: boolean
  error?: boolean
}

/** Panel state + the streamed conversation. ⌘K toggles the panel, Escape closes it. */
export function useAssist(context: AssistContext) {
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [turns, setTurns] = useState<AssistTurn[]>([])

  const contextRef = useRef(context)
  contextRef.current = context

  const patchLast = useCallback((fn: (turn: AssistTurn) => AssistTurn) => {
    setTurns((all) => all.map((turn, i) => (i === all.length - 1 ? fn(turn) : turn)))
  }, [])

  const ask = useCallback(
    (message: string) => {
      const question = message.trim()
      if (!question || busy) return
      setOpen(true)
      setBusy(true)
      setTurns((all) => [
        ...all,
        { question, answer: '', tools: [], sources: [], streaming: true },
      ])
      streamAssist(question, contextRef.current, {
        onTool: (tool) => patchLast((t) => ({ ...t, tools: [...t.tools, tool] })),
        onDelta: (text) => patchLast((t) => ({ ...t, answer: t.answer + text })),
        onMessage: (text, sources) => patchLast((t) => ({ ...t, answer: text || t.answer, sources })),
        onError: () => patchLast((t) => ({ ...t, error: true })),
      }).finally(() => {
        patchLast((t) => ({ ...t, streaming: false }))
        setBusy(false)
      })
    },
    [busy, patchLast],
  )

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        setOpen((o) => !o)
      } else if (e.key === 'Escape') {
        setOpen(false)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  return { open, setOpen, busy, turns, ask }
}
