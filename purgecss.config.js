module.exports = {
  content: ['resources/public/js/compiled/base.js', 'resources/public/index.html'],
  css: ['resources/public/css/main.min.css'],
  rejected: true,
  safelist: {
    standard: [
      'vote-arrow',
      /^startpage-step-/,
      /^masthead/,
      'speech-bubble-bordered',
      'product-page-feature-image',
      'navbar-bg-transparent-sm-white',
      'schnaq-navbar-skeleton',
      'modal-child',
      /^label-/,
      'feed-button-create',
      'feed-button',
      'feed-button-focused'],
    deep: [
      /^theming-enabled$/,
      /^image-container$/,
      /^lexical-editor-sm$/,
      /^lexical-editor$/,
      /^klaro$/],
    greedy: [/toast-header$/]
  }
}
