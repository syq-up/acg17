<template>
  <acg17-header></acg17-header>
  <router-view />
</template>

<script>
import Acg17Header from "../components/Acg17Header";

export default {
  name: "Acg17Container",
  components: {
    'acg17-header': Acg17Header,
  }
}
</script>

<style scoped>

</style>