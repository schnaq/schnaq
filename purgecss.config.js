module.exports = {
  content: ['resources/public/js/compiled/base.js', 'resources/public/index.html'],
  css: ['resources/public/css/main.min.css'],
  rejected: true,
  safelist: {
    deep: [/^theming-enabled$/]
  }
}
