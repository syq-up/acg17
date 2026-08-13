<template>
  <svg class="icon" aria-hidden="true">
    <use :xlink:href="icon"></use>
  </svg>
</template>

<script>
export default {
  name: "Icon",
  props: {
    icon: {
      type: String,
      required: true,
    },
  },
}
</script>

<style scoped>

</style>