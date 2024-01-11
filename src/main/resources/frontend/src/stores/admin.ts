import {defineStore} from "pinia";
import type {User} from "@/types/types";
import {computed, ref} from "vue";

export const useAdminStore = defineStore('admin', () => {
    const users = ref<User[]>([]);

    const admins = computed(() => users.value.filter(user => user.role === 'ADMIN'));

    const students = computed(() => users.value.filter(user => user.role === 'STUDENT'));

    return {users, admins, students}
});