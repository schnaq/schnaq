module.exports = {
  content: ['resources/public/js/compiled/base.js', 'resources/public/index.html'],
  css: ['resources/public/css/main.min.css'],
  rejected: true,
  safelist: {
    standard: [
      'feed-button',
      'feed-button-create',
      'feed-button-focused',
      'font-150',
      'modal-child',
      'navbar-bg-transparent-sm-white',
      'product-page-feature-image',
      'schnaq-navbar-skeleton',
      'speech-bubble-bordered',
      'vote-arrow',
      'wave-bottom-primary-and-secondary',
      /^highlight-card/,
      /^label-/,
      /^masthead/,
      /^rounded-/,
      /^startpage-step-/,
    ],
    deep: [
      /^image-container$/,
      /^klaro$/,
      /^lexical-editor$/,
      /^lexical-editor-sm$/,
      /^theming-enabled$/,
      /^tippy/,
    ],
    greedy: [/toast-header$/]
  }
}
