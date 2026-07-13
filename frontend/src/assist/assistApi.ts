// ACMEassist streaming client. The turn endpoint streams Server-Sent Events; native EventSource
// can't send the Authorization header, so we read the stream with fetch + ReadableStream and
// forward the Base bearer token (ADR-0008). Frames: `event:<type>\ndata:<json>\n\n`.

import { API_BASE, getAuthToken } from '../api/client'

export interface AssistContext {
  module: string
  subView?: string
  entityId?: string
}

export interface AssistHandlers {
  onTool?: (tool: string) => void
  onDelta?: (text: string) => void
  onMessage?: (text: string, sources: string[]) => void
  onError?: (error: Error) => void
}

interface AssistPayload {
  tool?: string
  detail?: string
  text?: string
  sources?: string[]
}

export async function streamAssist(
  message: string,
  context: AssistContext,
  handlers: AssistHandlers,
): Promise<void> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
  }
  const token = getAuthToken()
  if (token) headers['Authorization'] = `Bearer ${token}`

  let response: Response
  try {
    response = await fetch(`${API_BASE}/base/assist/messages`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ message, context }),
    })
  } catch (e) {
    handlers.onError?.(e instanceof Error ? e : new Error('network error'))
    return
  }

  if (!response.ok || !response.body) {
    handlers.onError?.(new Error(`assist → ${response.status}`))
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    let sep: number
    while ((sep = buffer.indexOf('\n\n')) >= 0) {
      dispatch(buffer.slice(0, sep), handlers)
      buffer = buffer.slice(sep + 2)
    }
  }
}

function dispatch(frame: string, handlers: AssistHandlers): void {
  let event = 'message'
  let data = ''
  for (const line of frame.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) data += line.slice(5).trim()
  }
  if (!data) return

  let payload: AssistPayload
  try {
    payload = JSON.parse(data) as AssistPayload
  } catch {
    return
  }

  switch (event) {
    case 'tool':
      if (payload.tool) handlers.onTool?.(payload.tool)
      break
    case 'delta':
      if (payload.text) handlers.onDelta?.(payload.text)
      break
    case 'message':
      handlers.onMessage?.(payload.text ?? '', payload.sources ?? [])
      break
    default:
      break
  }
}
