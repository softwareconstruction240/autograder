import {defineStore} from "pinia";
import type {User} from "@/types/types";
import {ref} from "vue";

export const useAdminStore = defineStore('admin', () => {
    const users = ref<User[]>([]);

    return {users}
});