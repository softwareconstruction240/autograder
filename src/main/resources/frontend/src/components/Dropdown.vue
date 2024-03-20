<script setup lang="ts">

</script>

<template>
  <div class="dropdown">
    <slot name="dropdown-parent" />
    <div class="dropdown-content">
      <slot name="dropdown-items" />
    </div>
  </div>
</template>

<style>
/* The container <div> - needed to position the dropdown content */
.dropdown {
  position: relative;
  display: inline-block;
}

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
.dropdown:hover .dropdown-content {display: block;}
</style>