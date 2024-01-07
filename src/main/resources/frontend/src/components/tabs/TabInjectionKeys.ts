import type {InjectionKey, Ref} from "vue";

export interface Tab {
    title: string;
    hash: string;
}

export const addTabKey = Symbol() as InjectionKey<(tab: Tab) => void>;
export const activeTabHashKey = Symbol() as InjectionKey<Ref<string>>;