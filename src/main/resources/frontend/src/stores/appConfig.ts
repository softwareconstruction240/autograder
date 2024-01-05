import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

type ImportMeta = {
    VITE_APP_BACKEND_HOST: string
}

// @ts-ignore
const env: ImportMeta = import.meta.env;
export const useAppConfigStore = defineStore('appConfig', () => {
  const backendHost = ref<string>(env.VITE_APP_BACKEND_HOST);

  return { backendHost }
})
