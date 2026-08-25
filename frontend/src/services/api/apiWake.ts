/**
 * Tracks whether the production API is likely cold-starting (Render free tier).
 * Used for a calm banner while the first request waits.
 */

type Listener = (waking: boolean) => void

let waking = false
const listeners = new Set<Listener>()

export function isApiWaking(): boolean {
  return waking
}

export function setApiWaking(next: boolean): void {
  if (waking === next) {
    return
  }
  waking = next
  listeners.forEach((listener) => listener(waking))
}

export function subscribeApiWaking(listener: Listener): () => void {
  listeners.add(listener)
  listener(waking)
  return () => {
    listeners.delete(listener)
  }
}
