import { ref, computed } from "vue";
import { defineStore } from "pinia";

type User = {
  repoUrl: string;
  netId: string;
  firstName: string;
  lastName: string;
  role: "STUDENT" | "ADMIN";
};

export const useUserStore = defineStore("user", () => {
  const user = ref<User | null>(null);

  const isLoggedIn = computed(() => user.value !== null);

  const isFullyRegistered = computed(() => {
    const user = useUserStore().user;
    if (user == null) {
      return false;
    }
    if (user.role == "STUDENT") {
      if (!user.repoUrl) {
        return false;
      }
    }
    return true;
  });

  return { user, isLoggedIn, isFullyRegistered };
});
