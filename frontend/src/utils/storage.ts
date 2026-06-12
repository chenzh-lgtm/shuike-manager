export const storage = {
  get(key: string): string | null { return localStorage.getItem(key) },
  set(key: string, value: string) { localStorage.setItem(key, value) },
  remove(key: string) { localStorage.removeItem(key) },
  getObj<T>(key: string): T | null {
    const v = localStorage.getItem(key)
    return v ? JSON.parse(v) : null
  },
  setObj(key: string, value: any) { localStorage.setItem(key, JSON.stringify(value)) }
}
