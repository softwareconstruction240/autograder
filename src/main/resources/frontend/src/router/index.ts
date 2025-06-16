import { createRouter, createWebHistory } from "vue-router";
import HomeView from "@/views/HomeView.vue";
import LoginView from "@/views/LoginView.vue";
import { useAuthStore } from "@/stores/auth";
import AdminView from "@/views/AdminView/AdminView.vue";
import RegisterView from "@/views/RegisterView.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "home",
      component: HomeView,
      beforeEnter: (to, from) => {
        if (!useAuthStore().isLoggedIn)
          // query must be included for login error messages to work correctly
          return { name: "login", query: to.query };
        if (!useAuthStore().isFullyRegistered) return "/register";
        if (useAuthStore().user?.role === "ADMIN") return "/admin";
      },
    },
    {
      path: "/register",
      name: "register",
      component: RegisterView,
      beforeEnter: (to, from) => {
        if (useAuthStore().isFullyRegistered) return "/";
        if (!useAuthStore().isLoggedIn) return "login";
      },
    },
    {
      path: "/admin",
      name: "admin",
      component: AdminView,
      beforeEnter: (to, from) => {
        if (useAuthStore().user?.role !== "ADMIN") return "/";
      },
    },
    {
      path: "/login",
      name: "login",
      component: LoginView,
    },
  ],
});

export default router;
