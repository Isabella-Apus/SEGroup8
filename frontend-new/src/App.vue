<template>
  <router-view v-slot="{ Component }">
    <transition name="route-fade">
      <component :is="Component" />
    </transition>
  </router-view>
</template>
