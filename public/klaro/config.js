// By default, Klaro will load the config from  a global "klaroConfig" variable.
// You can change this by specifying the "data-config" attribute on your
// script take, e.g. like this:
// <script src="klaro.js" data-config="myConfigVariableName" />
// You can also disable auto-loading of the consent notice by adding
// data-no-auto-load=true to the script tag.
var klaroConfig = {
  // You can customize the ID of the DIV element that Klaro will create
  // when starting up. If undefined, Klaro will use 'klaro'.
  elementID: 'klaro',

  // How Klaro should store the user's preferences. It can be either 'cookie'
  // (the default) or 'localStorage'.
  storageMethod: 'cookie',

  // You can customize the name of the cookie that Klaro uses for storing
  // user consent decisions. If undefined, Klaro will use 'klaro'.
  cookieName: 'meetly-analytics',

  // You can also set a custom expiration time for the Klaro cookie.
  // By default, it will expire after 120 days.
  cookieExpiresAfterDays: 365,

  // You can change to cookie domain for the consent manager itself.
  // Use this if you want to get consent once for multiple matching domains.
  // If undefined, Klaro will use the current domain.
  //cookieDomain: '.github.com',

  // Put a link to your privacy policy here (relative or absolute).
  privacyPolicy: '/datenschutz',

  // Defines the default state for applications (true=enabled by default).
  default: false,

  // If "mustConsent" is set to true, Klaro will directly display the consent
  // manager modal and not allow the user to close it before having actively
  // consented or declines the use of third-party apps.
  mustConsent: false,

  // Show "accept all" to accept all apps instead of "ok" that only accepts
  // required and "default: true" apps
  acceptAll: false,

  // replace "decline" with cookie manager modal
  hideDeclineAll: false,

  // You can define the UI language directly here. If undefined, Klaro will
  // use the value given in the global "lang" variable. If that does
  // not exist, it will use the value given in the "lang" attribute of your
  // HTML tag. If that also doesn't exist, it will use 'en'.
  //lang: 'en',

  // You can overwrite existing translations and add translations for your
  // app descriptions and purposes. See `src/translations/` for a full
  // list of translations that can be overwritten:
  // https://github.com/KIProtect/klaro/tree/master/src/translations

  // Example config that shows how to overwrite translations:
  // https://github.com/KIProtect/klaro/blob/master/src/configs/i18n.js
  translations: {
    // If you erase the "consentModal" translations, Klaro will use the
    // bundled translations.
    de: {
      consentModal: {
        title: 'Persönliche Einstellungen zu Cookies auf unserer Webseite',
        description:
          'Wir nutzen Cookies, um Ihnen die bestmögliche Nutzung unserer Webseite zu ermöglichen und unsere Kommunikation mit Ihnen zu verbessern. Wir berücksichtigen hierbei Ihre Präferenzen und verarbeiten Daten für Marketing und Analytics nur, wenn Sie explizit Ihr Einverständnis geben. Sie können Ihre Einwilligung jederzeit mit Wirkung für die Zukunft widerrufen.',
      },
      consentNotice: {
        description: 'Helfen Sie uns bei der Weiterentwicklung dieses Produkts! Indem Sie unsere Cookies erlauben, können wir Ihr Verhalten innerhalb unserer Webanwendung besser nachvollziehen. Ihre Daten werden stets vertraulich behandelt. Die Daten werden zu folgenden Zwecken erfasst: {purposes}',
      },
      acceptSelected: 'Ausgewählten zustimmen',
      ok: "Zustimmen",
      matomo: {
        description: 'Sammeln von Besucherstatistiken zur Verbesserung der Webseite',
      },
      purposes: {
        analytics: 'Statistiken',
      },

    },
    en: {
      consentModal: {
        description:
          'We use cookies to enable you the best possible use of our website and to improve our communication with you. We take your preferences into account and process data for marketing and analytics only if you explicitly give your consent. You can withdraw your consent at any time with effect for the future.',
      },
      consentNotice: {
        description: 'Please support the development of this product by accepting Cookies on this site. They enable us to verify the way you are using this software. All data is stored on our own servers. We are collecting the data for the following purposes: {purposes}',
      },
      matomo: {
        description: 'Collecting of visitor statistics for enhancing the website',
      },
      purposes: {
        analytics: 'Analytics',
      },
    },
  },

  // This is a list of third-party apps that Klaro will manage for you.
  apps: [
    {
      // Each app should have a unique (and short) name.
      name: 'matomo',

      // If "default" is set to true, the app will be enabled by default
      // Overwrites global "default" setting.
      // We recommend leaving this to "false" for apps that collect
      // personal information.
      default: true,

      // The title of you app as listed in the consent modal.
      title: 'Matomo (analytics.dialogo.io, self-hosted)',

      // The purpose(s) of this app. Will be listed on the consent notice.
      // Do not forget to add translations for all purposes you list here.
      purposes: ['analytics'],

      // A list of regex expressions or strings giving the names of
      // cookies set by this app. If the user withdraws consent for a
      // given app, Klaro will then automatically delete all matching
      // cookies.
      cookies: [
        // you can also explicitly provide a path and a domain for
        // a given cookie. This is necessary if you have apps that
        // set cookies for a path that is not "/" or a domain that
        // is not the current domain. If you do not set these values
        // properly, the cookie can't be deleted by Klaro
        // (there is no way to access the path or domain of a cookie in JS)
        [/^_pk_.*$/, '/', 'meetly.dialogo.io'], //for the production version
        [/^_pk_.*$/, '/', 'localhost'], //for the local version
        'piwik_ignore',
      ],

      // An optional callback function that will be called each time
      // the consent state for the app changes (true=consented). Passes
      // the `app` config as the second parameter as well.
      callback: function (consent, app) {
        // This is an example callback function.
      },

      // If "required" is set to true, Klaro will not allow this app to
      // be disabled by the user.
      required: false,

      // If "optOut" is set to true, Klaro will load this app even before
      // the user gave explicit consent.
      // We recommend always leaving this "false".
      optOut: false,

      // If "onlyOnce" is set to true, the app will only be executed
      // once regardless how often the user toggles it on and off.
      onlyOnce: true,
    },
  ],
}; 