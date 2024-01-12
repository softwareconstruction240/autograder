import {defineStore} from "pinia";
import type {User} from "@/types/types";
import {computed, ref} from "vue";
import {usersGet} from "@/services/adminService";

export const useAdminStore = defineStore('admin', () => {
    const users = ref<User[]>([]);

    const updateUsers = async () => {
        users.value = await usersGet();
    }

    const admins = computed(() => users.value.filter(user => user.role === 'ADMIN'));

    const students = computed(() => users.value.filter(user => user.role === 'STUDENT'));

    return {users, admins, students, updateUsers}
});