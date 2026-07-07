import { ref, watchEffect } from 'vue'

const isDark = ref(localStorage.getItem('theme') !== 'light')

export function useTheme() {
  const toggle = () => {
    isDark.value = !isDark.value
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  }

  watchEffect(() => {
    document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
  })

  return { isDark, toggle }
}