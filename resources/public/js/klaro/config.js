// By default, Klaro will load the config from  a global "klaroConfig" variable.
// You can change this by specifying the "data-config" attribute on your
// script take, e.g. like this:
// <script src="klaro.js" data-config="myConfigVariableName" />
var klaroConfig = {
  // With the 0.7.0 release we introduce a 'version' paramter that will make
  // if easier for us to keep configuration files backwards-compatible in the future.
  version: 1,

  // You can customize the ID of the DIV element that Klaro will create
  // when starting up. If undefined, Klaro will use 'klaro'.
  elementID: 'klaro',

  // You can override CSS style variables here. For IE11, Klaro will
  // dynamically inject the variables into the CSS. If you still consider
  // supporting IE9-10 (which you probably shouldn't) you need to use Klaro
  // with an external stylesheet as the dynamic replacement won't work there.
  styling: {
    theme: ['light', 'top', 'wide'],
  },

  // Setting this to true will keep Klaro from automatically loading itself
  // when the page is being loaded.
  noAutoLoad: false,

  // Setting this to true will render the descriptions of the consent
  // modal and consent notice are HTML. Use with care.
  htmlTexts: true,

  // Setting 'embedded' to true will render the Klaro modal and notice without
  // the modal background, allowing you to e.g. embed them into a specific element
  // of your website, such as your privacy notice.
  embedded: false,

  // You can group services by their purpose in the modal. This is advisable
  // if you have a large number of services. Users can then enable or disable
  // entire groups of services instead of having to enable or disable every service.
  groupByPurpose: true,

  // How Klaro should store the user's preferences. It can be either 'cookie'
  // (the default) or 'localStorage'.
  storageMethod: 'cookie',

  // You can customize the name of the cookie that Klaro uses for storing
  // user consent decisions. If undefined, Klaro will use 'klaro'.
  cookieName: 'schnaq-analytics',

  // You can also set a custom expiration time for the Klaro cookie.
  // By default, it will expire after 120 days.
  cookieExpiresAfterDays: 365,

  // You can change to cookie domain for the consent manager itself.
  // Use this if you want to get consent once for multiple matching domains.
  // If undefined, Klaro will use the current domain.
  //cookieDomain: '.github.com',

  // You can change to cookie path for the consent manager itself.
  // Use this to restrict the cookie visibility to a specific path.
  // If undefined, Klaro will use '/' as cookie path.
  //cookiePath: '/',

  // Defines the default state for services (true=enabled by default).
  default: false,

  // If "mustConsent" is set to true, Klaro will directly display the consent
  // manager modal and not allow the user to close it before having actively
  // consented or declines the use of third-party services.
  mustConsent: true,

  // Show "accept all" to accept all services instead of "ok" that only accepts
  // required and "default: true" services
  acceptAll: true,

  // replace "decline" with cookie manager modal
  hideDeclineAll: true,

  // hide "learnMore" link
  hideLearnMore: false,

  // show cookie notice as modal
  noticeAsModal: false,

  // you can specify an additional class (or classes) that will be added to the Klaro `div`
  //additionalClass: 'my-klaro',

  // You can define the UI language directly here. If undefined, Klaro will
  // use the value given in the global "lang" variable. If that does
  // not exist, it will use the value given in the "lang" attribute of your
  // HTML tag. If that also doesn't exist, it will use 'en'.
  //lang: 'en',

  // You can overwrite existing translations and add translations for your
  // service descriptions and purposes. See `src/translations/` for a full
  // list of translations that can be overwritten:
  // https://github.com/KIProtect/klaro/tree/master/src/translations

  // Example config that shows how to overwrite translations:
  // https://github.com/KIProtect/klaro/blob/master/src/configs/i18n.js
  translations: {
    // translations defined under the 'zz' language code act as default
    // translations.
    zz: {
      privacyPolicyUrl: '/privacy/extended',
    },
    // If you erase the "consentModal" translations, Klaro will use the
    // bundled translations.
    de: {
      privacyPolicyUrl: '/de/privacy/extended',
      acceptSelected: 'Speichern',
      consentModal: {
        title: "<img src='https://s3.disqtec.com/schnaq-common/logos/schnaqqifant.svg' width='75px'> Cookies",
        description: 'Wir verwenden Cookies :-) Einige sind für Statistiken, andere für essenzielle Funktionen der Anwendung.',
      },
      privacyPolicy: {
        text: 'Mehr Informationen: {privacyPolicy}'
      },
      matomo: {
        description: 'Erfassen von anonymen Besucher:innenstatistiken',
      },
      purposes: {
        advertising: 'Anzeigen von Werbung',
        analytics: 'Analysen',
        essential: 'Essenziell',
        livechat: 'Live Chat',
        security: 'Sicherheit',
        styling: 'Styling',
      },
    },
    en: {
      consentModal: {
        title: "<img src='https://s3.disqtec.com/schnaq-common/logos/schnaqqifant.svg' alt='schnaqqifant' width='75px'> Cookies",
        description:
          'We use cookies :-) Some are for statistics, others for essential functions of the application.',
      },
      privacyPolicy: {
        text: 'More information: {privacyPolicy}'
      },
      matomo: {
        description: 'Collecting of anonymous visitor statistics',
      },
      purposes: {
        advertising: 'Advertising',
        analytics: 'Analytics',
        essential: 'Essential',
        livechat: 'Livechat',
        security: 'Security',
        styling: 'Styling',
      },
    },
  },

  // This is a list of third-party services that Klaro will manage for you.
  services: [
    {
      // Each service should have a unique (and short) name.
      name: 'matomo',

      // If "default" is set to true, the service will be enabled by default
      // Overwrites global "default" setting.
      // We recommend leaving this to "false" for services that collect
      // personal information.
      default: true,

      // The title of you service as listed in the consent modal.
      title: 'Matomo (analytics.schnaq.com, self-hosted)',

      // The purpose(s) of this service. Will be listed on the consent notice.
      // Do not forget to add translations for all purposes you list here.
      purposes: ['essential'],

      // A list of regex expressions or strings giving the names of
      // cookies set by this service. If the user withdraws consent for a
      // given service, Klaro will then automatically delete all matching
      // cookies.
      cookies: [
        // you can also explicitly provide a path and a domain for
        // a given cookie. This is necessary if you have services that
        // set cookies for a path that is not "/" or a domain that
        // is not the current domain. If you do not set these values
        // properly, the cookie can't be deleted by Klaro
        // (there is no way to access the path or domain of a cookie in JS)
        // Notice that it is not possible to delete cookies that were set
        // on a third-party domain! See the note at mdn:
        // https://developer.mozilla.org/en-US/docs/Web/API/Document/cookie#new-cookie_domain
        [/^_pk_.*$/, '/', 'schnaq.com'], //for the production version
        [/^_pk_.*$/, '/', 'localhost'], //for the local version
        'piwik_ignore',
      ],

      // An optional callback function that will be called each time
      // the consent state for the service changes (true=consented). Passes
      // the `service` config as the second parameter as well.
      callback: function (consent, service) {
        // This is an example callback function.
        // To be used in conjunction with Matomo 'requireCookieConsent' Feature, Matomo 3.14.0 or newer
        // For further Information see https://matomo.org/faq/new-to-piwik/how-can-i-still-track-a-visitor-without-cookies-even-if-they-decline-the-cookie-consent/
        // if (consent === true) {
        //   _paq.push(['rememberCookieConsentGiven']);
        // } else {
        //   _paq.push(['forgetCookieConsentGiven']);
        // }
      },

      // If "required" is set to true, Klaro will not allow this service to
      // be disabled by the user.
      required: true,

      // If "optOut" is set to true, Klaro will load this service even before
      // the user gave explicit consent.
      // We recommend always leaving this "false".
      optOut: false,

      // If "onlyOnce" is set to true, the service will only be executed
      // once regardless how often the user toggles it on and off.
      onlyOnce: true,
    },
    {
      name: 'schnaq',
      title: 'schnaq Funktionen',
      description: 'Für den Betrieb notwendige Einstellungen, wenn man sich bspw. einen Benutzer:innenaccount erstellt.',
      purposes: ['essential'],
      required: true,
    },
    {
      name: 'facebook-pixel',
      title: 'Facebook Pixel',
      description: 'Cookie von Facebook, verwendet für zielgruppenorientierte Werbung und Anzeigenmessungen.',
      purposes: ['analytics'],
    },
  ],
};
