<script setup lang="ts">
const { disabled } = defineProps<{
  disabled: boolean;
}>();

</script>

<template>
  <div :class="{ 'dropdown-main': !disabled, 'disabled-dropdown': disabled }">
    <slot name="dropdown-parent" />
    <div class="dropdown-content">
      <slot name="dropdown-items" />
    </div>
  </div>
</template>

<style>
/* Dropdown Content (Hidden by Default) */
.dropdown-content {
  display: none;
  position: absolute;
  background-color: #f1f1f1;
  min-width: 160px;
  box-shadow: 0 8px 16px 0 rgba(0,0,0,0.2);
  z-index: 10;
}

/* Links inside the dropdown */
.dropdown-content * {
  color: black;
  padding: 12px 16px;
  text-decoration: none;
  display: block;
}

/* Change color of dropdown links on hover */
.dropdown-content *:hover {
  background-color: #adadad;
  cursor: pointer;
}

/* Show the dropdown menu on hover */
.dropdown-main:hover .dropdown-content {display: block;}

/* grey out the button when disabled */
.disabled-dropdown {
  opacity: 40%;
  cursor: not-allowed;
}

.dropdown-main:hover .disabled-dropdown .dropdown-content {display: none}
</style>