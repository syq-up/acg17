<template>
  <div class="footer">
    <icon icon="#icon-rabbit"></icon>
  </div>
</template>

<script>
export default {
  name: "Acg17Footer",
}
</script>

<style scoped>
.footer {
  height: 200px;
  display: flex;
  justify-content: center;
  align-items: flex-end;
  overflow: hidden;
}
.footer .icon {
  width: 24px;
  height: 24px;
  fill: rgba(0, 0, 0, 0.16);
  margin-bottom: -16px;
  transform: scale(3);
  transform-origin: center bottom;
}
</style>