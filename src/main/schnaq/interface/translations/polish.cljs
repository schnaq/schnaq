(ns schnaq.interface.translations.polish
  (:require [schnaq.interface.config :refer [marketing-num-schnaqs marketing-num-statements]]
            [schnaq.interface.utils.toolbelt :as toolbelt]))

(def labels
  {:error/export-failed "Eksport nie zadziałał, spróbuj ponownie później."

   :nav/schnaqs "schnaqs"
   :nav.schnaqs/show-all "Wszystkie schnaqs"
   :nav.schnaqs/show-all-public "Wszystkie publiczne schnaqs"
   :nav.schnaqs/create-schnaq "Utwórz schnaq"
   :nav.schnaqs/last-added "Ostatnio utworzony schnaq"
   :nav/blog "Blog"
   :nav/admin "Admin"
   :nav.buttons/language-toggle "Zmiana języka"

   ;; Alphazulu Page
   :alphazulu.page/heading "Alphazulu"
   :alphazulu.page/subheading "Nowoczesna praca dla nowoczesnych firm"
   :alphazulu.introduction/title "Nowe prace z Niemiec"
   :alphazulu.introduction/body
   [:<>
    [:p "Bezpiecznie, indywidualnie, lokalnie: to jest siła ALPHAZULU."]
    [:p "Znajdź rozwiązanie dopasowane do potrzeb Twojej firmy dzięki modułom "
     [:a {:href "/"} "schnaq"] ", " [:a {:href "https://wetog.de/"} "WETOG"] ", "
     [:a {:href "https://xignsys.com/"} "XignSys"] ", " [:a {:href "https://www.cobago.de/"} "Cobago"] ", "
     [:a {:href "https://www.trustcerts.de/"} "TrustCerts"] " i " [:a {:href "https://ec3l.com/"} "EC3L"] "."]
    [:p "Wszystkie produkty Alphazulu mogą być łączone i integrowane ze sobą."]]
   :alphazulu.schnaq/title "Ustrukturyzowana wymiana wiedzy"
   :alphazulu.schnaq/body
   [:<>
    [:p "Zarządzaj cyfrową transformacją swojej firmy. Pomagamy w komunikacji wewnętrznej i dzieleniu się wiedzą."]
    [:p "Użyj samodzielnego schnaq i zaloguj się przez Xign.Me, na przykład, lub zarezerwuj schnaq bezpośrednio z subskrypcją Wetog.."]]
   :alphazulu.wetog/title "Bezpieczna współpraca"
   :alphazulu.wetog/body
   [:<>
    [:p "Wetog wykorzystuje komputer kwantowy LIQRYPT do bezpiecznego szyfrowania wszystkich danych, czatów i wideokonferencji.."]
    [:p "Masz już konto na Wetog? Zarezerwuj schnaq bezpośrednio w Wetog. Albo jeden z pozostałych partnerów Alphazulu.."]]
   :alphazulu.xignsys/title "Logowanie bez hasła z dowolnego miejsca"
   :alphazulu.xignsys/body
   [:<>
    [:p "Dzięki unikalnemu rozwiązaniu Xign.Me, możesz uwierzytelnić się w dowolnym miejscu za pomocą swoich danych biometrycznych bez konieczności podawania hasła.."]
    [:p "Spróbuj Xign.Me po zalogowaniu się do schnaq. Loguj się do partnerów Alphazulu bez konieczności stosowania wrażliwych haseł."]]
   :alphazulu.cobago/title "Cyfrowa asystentura"
   :alphazulu.cobago/body
   [:<>
    [:p "Cobago pomaga Twojej firmie łatwo zautomatyzować formularze i procesy. Bez żadnej wiedzy technicznej."]
    [:p "Chcesz używać dyskusji schnaq w ramach platformy Cobago? Daj nam znać, aby uzyskać dostęp!"]]
   :alphazulu.trustcerts/title "Podpis cyfrowy - zasilany przez Blockchains"
   :alphazulu.trustcerts/body
   [:<>
    [:p "Dzięki rozwiązaniom TrustCerts możesz łatwo i bezpiecznie podpisywać i weryfikować ważne dokumenty za pośrednictwem blockchain.."]
    [:p "Czy decyzja podjęta przez schnaqa musi być wiążąco zapisana, czy też dokument z Wetog lub Cobago Sixpad musi być podpisany. Alphazulu dostarcza rozwiązanie."]]
   :alphazulu.ec3l/title "Kształcenie ustawiczne z pomysłem"
   :alphazulu.ec3l/body
   [:<>
    [:p "Nowoczesne, ukierunkowane szkolenia, których skuteczność została potwierdzona, przeniosą Twoją wiedzę biznesową na wyższy poziom.."]
    [:p "Wkrótce znajdziesz również moduły schnaq w swoim szkoleniu EC3L. Po prostu używaj tego, co już wiesz."]]
   :alphazulu.activate/title "Osobista rozmowa"
   :alphazulu.activate/body [:p "Jeśli chciałbyś dowiedzieć się więcej o schnaq i Alphazulu, zarezerwuj spotkanie z nami przez. "
                             [:a {:href "https://calendly.com/schnaq/30min"} "Calendly."] " Cieszymy się na osobistą rozmowę."]

   ;; Call to contribute
   :call-to-contribute/lead "Nie ma tu jeszcze żadnych wypowiedzi"
   :call-to-contribute/body "Zacznij od swojego pierwszego postu"

   ;; code of conduct
   :coc/heading "Zasady postępowania"
   :coc/subheading "Nasze zasady etykiety"

   :coc.users/lead "Zachowanie wobec innych użytkowników"
   :coc.users/title "Traktowanie z szacunkiem i niedyskryminacja"
   :coc.users/body "Wzajemny szacunek jest ważny, abyśmy mogli żyć razem i stanowi podstawę do obiektywnych dyskusji. Dotyczy to nie tylko offline, ale również online. \nWażne jest dla nas, aby każdy użytkownik mógł wyrazić siebie, nie będąc dyskryminowanym ze względu na swoją osobę, pochodzenie lub poglądy. \nPosty, które nie są zgodne z tymi wytycznymi będą usuwane."

   :coc.content/lead "Spis treści"
   :coc.content/title "Przestrzegamy prawa, proszę robić to samo."
   :coc.content/body "Przestrzegamy niemieckiej Ustawy Zasadniczej; dotyczy to również i w szczególności ochrony danych, równouprawnienia i niedyskryminacji.\nTreści, które naruszają obowiązujące prawo, będą przez nas usuwane."

   ;; how-to
   :how-to/button "Jak używać schnaq?"
   :how-to/title "Jak używać schnaq?"
   :how-to.create/title "O czym chciałbyś porozmawiać ze swoim zespołem?"
   :how-to.create/body
   [:<>
    [:p [:i "\"Jakich systemów używamy i kiedy?\", \"Jaką cenę oferujemy instytucjom edukacyjnym?\", \"Jaka jest nasza wizja?\"."]]
    [:p "Nadaj swojemu schnaqowi znaczący tytuł i pozwól całemu zespołowi dyskutować. Wszystkie wypowiedzi są ważne i oferują indywidualne spojrzenie na dyskusję."]]
   :how-to.why/title "Dlaczego powinienem schnaq?"
   :how-to.why/body "Straciłeś wątek? Nie miałeś szansy się wypowiedzieć? Czy wszyscy wszystko zrozumieli? Po raz kolejny nie mam pojęcia dlaczego było tyle dyskusji?\nOszczędzaj czas i zdobywaj wiedzę dzięki schnaq. Weź udział, gdy masz czas. Mapa myśli z dyskusji jest darmowa."
   :how-to.admin/title "Zarządzaj swoim schnaqiem lub uzyskaj pomoc"
   :how-to.admin/body "Zaproś uczestników poprzez link lub email. Jeśli potrzebujesz pomocy w administracji, po prostu kliknij na \"Dostęp dla administratora\" i wyślij konto administracyjne przez e-mail.\n\nJako administrator, możesz usuwać posty i zapraszać ludzi przez e-mail."
   :how-to.schnaq/title "Jak używać schnaq?"
   :how-to.schnaq/body "Podziel się swoją opinią! Wprowadź go w polu wprowadzania danych, a pojawi się on na liście wpłat. Uczestnicy mogą na nią reagować i odpowiadać. Mapa myśli jest generowana automatycznie i aktualizuje się wraz z każdym nowym postem. Jeśli chcesz przejść do jakiegoś postu, po prostu kliknij na niego."
   :how-to.pro-con/title "Czy jesteś za, czy przeciw?"
   :how-to.pro-con/body "Podziel się z innymi swoją opinią na temat bieżącego postu. Klikając na nasz przycisk za/przeciw obok pola wprowadzania danych, możesz zmienić swoje stanowisko. Można też podać kilka argumentów za lub przeciw. Argumenty \"za\" są oznaczone kolorem niebieskim, a \"przeciw\" kolorem pomarańczowym."
   :how-to.call-to-action/title "Teraz już wiesz!"
   :how-to.call-to-action/body "To wyjaśnia wszystko, co jest do wyjaśnienia na temat schnaq, więc zacznij od razu!"
   :how-to/ask-question "Nie jesteś pewien jak używać schnaq?"
   :how-to/ask-question-2 "Jakieś pytania?"
   :how-to/answer-question "Spójrz tutaj!"
   :how-to/question-dont-show-again "Rozumiesz?"
   :how-to/answer-dont-show-again "Nie wyświetlać w przyszłości!"
   :how-to/back-to-start "Powrót do schnaq"

   ;; Startpage
   :startpage/heading "Mózg słonia dla Twojego biznesu"
   :startpage/subheading "Gromadzenie wiedzy w sposób zrównoważony i bezpieczne zarządzanie nią"
   :startpage.social-proof/numbers [:span "schnaq pomógł poprowadzić ponad " [:b marketing-num-schnaqs]
                                    " dyskusji i Q&A, co przekłada się na " [:b marketing-num-statements] " przypadków wspólnej wiedzy."]

   :startpage.usage/lead "Do czego mogę używać schnaq?"
   :startpage.features/more-information "Więcej informacji"

   :startpage.information.know-how/title "Bezproblemowy transfer wiedzy"
   :startpage.information.know-how/body "Dyskusje i Q&A Schnaq pomagają Twojej firmie, kursowi i warsztatowi przekazywać wiedzę w mgnieniu oka. Doskonałość powstaje tam, gdzie wiedza przepływa swobodnie."

   :startpage.information.positioning/title "Zrozumieć, co jest przedmiotem dyskusji"
   :startpage.information.positioning/body "Dzięki automatycznie generowanym mindmapom i analizom A.I. każda dyskusja staje się jasna i łatwa do zrozumienia."

   :startpage.information.anywhere/title "Używaj schnaq w dowolnym miejscu i czasie"
   :startpage.information.anywhere/body "Schnaq działa jako aplikacja webowa na wszystkich popularnych systemach operacyjnych, przeglądarkach i urządzeniach. Nieważne czy smartfon, tablet czy komputer."

   :startpage.information.meetings/title "Nie wszystko musi być spotkaniem"
   :startpage.information.meetings/body "Zapomnij o spotkaniach i spotkaniach w kawiarni tylko po to, aby zadawać pytania. Dzięki inteligentnym Q&A możesz być na bieżąco bez spotkań, a nawet online!"

   :startpage.feature-box.know-how/title "Bezpieczne know-how"
   :startpage.feature-box.know-how/body
   "Cyfryzacja i mobilne biura zmieniły sposób, w jaki się komunikujemy.
   Jednak nawet w przypadku nowoczesnych procesów, wyzwaniem pozostaje łączenie wiedzy w ramach przedsiębiorstwa.
   Dzięki naszemu produktowi oferujemy Państwu rozwiązanie do komunikacji w nowoczesny sposób oraz połączenie know-how wszystkich ekspertów."
   :startpage.feature-box.discussion/title "Demokratyzacja dyskusji"
   :startpage.feature-box.discussion/body
   "Ludzie, którzy dużo mówią, nie muszą mieć racji.
   I odwrotnie, najwięksi geniusze w swojej dziedzinie są czasami raczej introwertyczni i nie lubią rozmawiać przy innych ludziach.
   Dzięki schnaq, kładziemy temu kres.
   Eksperci mogą wnieść swoje know-how i wziąć udział w konstruktywnej dyskusji - nawet bez wielu słów."
   :startpage.feature-box.learnings/title "Wykorzystanie zdobytych doświadczeń"
   :startpage.feature-box.learnings/body
   "Nowoczesne procesy mają pewien haczyk: dokumentację.
   Często wnioski są generowane, ale potem kończą w najlepszym razie w protokołach, które są archiwizowane, ale nigdy więcej nie są otwierane.
   Schnaq tworzy żywy system zarządzania wiedzą, który zaprasza Cię do pogłębiania wiedzy i odkrywania nowych ścieżek."
   :startpage.feature-box/explore-schnaq "Odkryj schnaq"

   :startpage.early-adopter/title "Teraz jesteś ciekaw?"
   :startpage.early-adopter/body "Bądź jedną z pierwszych osób korzystających z hubów schnaq"
   :startpage.early-adopter/or "lub"
   :startpage.early-adopter/test "Testowanie nowych funkcji"

   :startpage.newsletter/heading "Bądź jednym z pierwszych, którzy skorzystają z nowych funkcji!"
   :startpage.newsletter/button "Poproś o ekskluzywne informacje!"
   :startpage.newsletter/address-placeholder "Adres e-mail"
   :startpage.newsletter/consent "Chciałbym zapisać się do newslettera schnaq i otrzymywać regularnie informacje od schnaq.com w przyszłości."
   :startpage.newsletter/more-info-clicker "Przetwarzanie danych"
   :startpage.newsletter/policy-disclaimer "schnaq gromadzi, przetwarza i wykorzystuje dane osobowe, które podałeś powyżej w celu
   rozpatrzenie wniosku. W każdej chwili możesz zrezygnować z otrzymywania newslettera poprzez
   klikając na link podany w e-mailu. Możesz również wysłać do nas wiadomość e-mail
   a my zajmiemy się Twoim zgłoszeniem."
   :startpage.newsletter/privacy-policy-lead "Więcej informacji na temat przetwarzania danych osobowych można znaleźć w naszym"

   :startpage.faq/title "Najczęściej zadawane pytania"
   :startpage.faq.data/question "Co dzieje się z moimi danymi?"
   :startpage.faq.data/answer-1 "Aby zapewnić możliwie najbezpieczniejszą ochronę danych, przechowujemy wszystkie
   wszystkie dane przechowujemy wyłącznie na niemieckich serwerach. Wszystkie szczegóły indywidualnie i zrozumiale podsumowaliśmy w naszym"
   :startpage.faq.data/link-name "Polityka prywatności"
   :startpage.faq.data/answer-2 "."
   :startpage.faq.integration/question "Czy mogę zintegrować schnaq z moim istniejącym oprogramowaniem?"
   :startpage.faq.integration/answer "Obecnie schnaq może być zintegrowany z WETOG za pomocą kliknięcia myszką. Ciężko pracujemy nad integracją z Slackiem, MS Team i innymi popularnymi programami do komunikacji.
   Jeśli chcesz być informowany natychmiast, gdy integracja wejdzie w życie, zapisz się na listę"
   :startpage.faq.integration/link-name "Biuletyn informacyjny do."
   :startpage.faq.costs/question "Czy są jakieś ukryte koszty?"
   :startpage.faq.costs/answer "schnaq jest obecnie w fazie testowej i może być używany bezpłatnie. Nie ma żadnych kosztów. Jesteśmy szczęśliwi
   w zamian za szczerą informację zwrotną."
   :startpage.faq.start/question "Jak mogę zacząć używać schnaq?"
   :startpage.faq.start/answer "Możesz używać schnaq anonimowo lub zarejestrować się i zalogować, aby przeglądać i zarządzać swoimi schnaqami i postami z dowolnego miejsca.
   z dowolnego miejsca na świecie. Po prostu wypróbuj go i"
   :startpage.faq.start/link-name "uruchomić schnaq."
   :startpage.faq.why/question "Dlaczego powinienem używać schnaq?"
   :startpage.faq.why/answer "schnaq jest dla Ciebie, jeśli popierasz nowoczesną, otwartą i równą kulturę pracy.
   Naszym celem jest uelastycznienie komunikacji i dzielenia się wiedzą w miejscu pracy. W ten sposób
   nie tylko potencjał poszczególnych członków zespołu, ale także całej firmy."

   :startpage.founders-note/title "List od założycieli"

   ;; Login Page
   :page.login/heading "Proszę się zalogować"
   :page.login/subheading "Musisz być zalogowany, aby uzyskać dostęp do następujących zasobów"

   :page.beta/heading "Funkcja beta"
   :page.beta/subheading "Ta funkcja jest dostępna tylko dla beta testerów. Prosimy o zalogowanie się, jeśli należysz do grupy."

   :footer.buttons/about-us "O nas"
   :footer.buttons/legal-note "Impressum"
   :footer.buttons/privacy "Ochrona danych"
   :footer.buttons/press-kit "Naciśnij"
   :footer.buttons/publications "Publikacje"
   :footer.tagline/developed-with "Opracowano z"
   :footer.sponsors/heading "Nasze serwery są hostowane przez"
   :footer.registered/rights-reserved "Wszelkie prawa zastrzeżone"
   :footer.registered/is-registered "jest zastrzeżonym znakiem towarowym"

   ;; Header image
   :schnaq.header-image.url/placeholder "Wprowadź adres URL obrazu"
   :schnaq.header-image.url/button "Dodaj miniaturkę"
   :schnaq.header-image.url/note "Dozwolone są tylko treści z pixabay.com"
   :schnaq.header-image.url/label "Dodaj miniaturkę do swojego schnaq"
   :schnaq.header-image.url/successful-set "Obraz podglądu ustawiony pomyślnie"
   :schnaq.header-image.url/successful-set-body "Zdjęcie jest teraz wyświetlane w przeglądzie."
   :schnaq.header-image.url/failed-setting-title "Błąd podczas dodawania obrazu"
   :schnaq.header-image.url/failed-setting-body "Obraz nie jest używany w podglądzie."

   ;; Create schnaq
   :schnaq.create.dispatch/heading "Co chcesz zacząć?"
   :schnaq.create.dispatch/qanda "Pytania i odpowiedzi"
   :schnaq.create.dispatch.qanda/explain "Zbieraj pytania podczas kursu i odpowiadaj na nie, kiedy będziesz miał czas."
   :schnaq.create.dispatch.qanda/share "Zaproś uczestników poprzez link lub kod."
   :schnaq.create.dispatch/discussion "Dyskusja"
   :schnaq.create.dispatch.discussion/explain "Prowadź wspomagane przez AI i uporczywe dyskusje z innymi."
   :schnaq.create.dispatch.discussion/share "Zaproś uczestników poprzez link."

   :schnaq.create.input/title "O czym chcesz rozmawiać?"
   :schnaq.create.qanda.input/title "Czego powinny dotyczyć pytania?"
   :schnaq.create.input/placeholder "Ustal temat"
   :schnaq.create.hub/help-text "Dodaj swój schnaq bezpośrednio do huba."
   :schnaq/copy-link-tooltip "Kliknij tutaj, aby skopiować link"
   :schnaq/link-copied-heading "Link skopiowany"
   :schnaq/link-copied-success "Link został skopiowany do schowka!"
   :schnaq/created-success-heading "Twój schnaq został utworzony!"
   :schnaq/created-success-subheading "Teraz możesz rozesłać link dostępu lub zaprosić inne osoby przez e-mail 🎉"
   :schnaqs/continue-with-schnaq-after-creation "Wszyscy zaproszeni? Ruszamy!"
   :schnaqs/continue-to-schnaq-button "Do schnaq"

   :schnaq.admin/addresses-label "Adresy e-mail uczestników"
   :schnaq.admin/addresses-placeholder "Wpisz adresy e-mail oddzielone spacjami lub przerwami między wierszami."
   :schnaq.admin/addresses-privacy "Adresy te są wykorzystywane wyłącznie do wysyłania poczty i są następnie natychmiast usuwane z naszych serwerów."
   :schnaq.admin/send-invites-button-text "Wysyłanie zaproszeń"
   :schnaq.admin/send-invites-heading "Zaproś uczestników za pośrednictwem poczty elektronicznej"
   :schnaq.admin.notifications/emails-successfully-sent-title "Poczta(y) wysłana!"
   :schnaq.admin.notifications/emails-successfully-sent-body-text "Twoja poczta została pomyślnie wysłana."
   :schnaq.admin.notifications/sending-failed-title "Błąd dostawy!"
   :schnaq.admin.notifications/sending-failed-lead "Zaproszenie nie mogło zostać dostarczone na następujące adresy:"
   :schnaq.admin.notifications/statements-deleted-title "Wiadomości usunięte!"
   :schnaq.admin.notifications/statements-deleted-lead "Twoje wybrane wiadomości zostały pomyślnie usunięte."
   :schnaq.admin.notifications/heading "Ustawienia"
   :schnaq.admin.configurations.read-only/checkbox "Aktywuj ochronę przed zapisem"
   :schnaq.admin.configurations.read-only/explanation "Aktywuj, aby nie zezwalać na nowe posty. Istniejące posty są nadal widoczne i mogą być nadal analizowane. Opcja ta może być zmieniona w dowolnym momencie."
   :schnaq.admin.configurations.disable-pro-con/label "Za / przeciw Przycisk ukrycia"
   :schnaq.admin.configurations.disable-pro-con/explanation "Aktywuj, aby przycisk \"za/ przeciw\" nie był już wyświetlany. Nowe wkłady są traktowane jako zgoda. Opcja ta może być zmieniona w dowolnym momencie."

   :statement/reply "Odpowiedzi"
   :statement.edit.send.failure/title "Zmiana nie została zapisana"
   :statement.edit.send.failure/body "Zmiana nie mogła zostać wprowadzona. Proszę spróbować ponownie za chwilę."
   :statement.edit/label "Edytuj post"
   :statement.edit.button/submit "Prześlij"
   :statement.edit.button/cancel "Anuluj"
   :schnaq.edit/label "Edytuj tytuł"

   ;; schnaq creation
   :schnaq.create/title "Uruchomienie schnaq"
   :schnaq.create/heading "Zacznij od swojego schnaq."
   :schnaq.create/subheading "Dzięki schnaqowi możesz pozwolić swojemu zespołowi dyskutować i zabezpieczać zrównoważone decyzje."
   :schnaq.create.qanda/subheading "Dzięki schnaq, możesz mieć zoptymalizowane Q&A."
   :schnaq.create/info "Nadaj swojemu tematowi tytuł, który jest tak prosty i zrozumiały, jak to tylko możliwe."
   :schnaq.create.button/save "Uruchomienie schnaq"

   ;; schnaq value
   :schnaq.value/title "Jesteś gotowy do pracy"
   :schnaq.value/subtitle "Kilka wskazówek, które pomogą Tobie i Twojemu zespołowi:"
   :schnaq.value.security/title "Bezpieczeństwo danych"
   :schnaq.value.security/text "Ochrona danych jest dla nas ważna! Twoje dane są bezpieczne na niemieckich serwerach."
   :schnaq.value.respect/title "Dyskusja z szacunkiem"
   :schnaq.value.respect/text "Pełna szacunku interakcja jest ważna, aby móc żyć ze sobą i stanowi podstawę do obiektywnych dyskusji."
   :schnaq.value.share/title "Podziel się swoją dyskusją"
   :schnaq.value.share/text "Wystarczy zaprosić uczestników poprzez link lub e-mail. Rejestracja nie jest konieczna!"
   :schnaq.value.private/title "Zawsze prywatny"
   :schnaq.value.private/text "Domyślnie, twoje schnaqs są widoczne tylko dla ciebie i osób, z którymi się dzielisz."
   :schnaq.value.cards/title "Mindmap"
   :schnaq.value.cards/text "Dla lepszego przeglądu, nasza mindmapa jest generowana automatycznie."
   :schnaq.value.results/title "Widok wyników"
   :schnaq.value.results/text "Zobacz podsumowania i analizy swojej dyskusji (funkcja beta)."

   ;; Discussion Creation
   :discussion.create.hub-exclusive-checkbox/title "Dodaj Schnaqa do huba"
   :discussion.create.hub-exclusive-checkbox/label "Dodaj do hubu"

   ;; Discussion Dashboard
   :dashboard/posts "Posty"
   :dashboard/members "Członkowie"
   :dashboard/summary "Krótkie podsumowanie"
   :dashboard/top-posts "Top Posts"

   :discussion.navbar/title "Tytuł"
   :discussion.navbar/posts "Posty"
   :discussion.navbar/members "Członkowie"
   :discussion.navbar/views "widok"
   :discussion.state/read-only-label "tylko do odczytu"
   :discussion.state/read-only-warning "Ta dyskusja jest tylko do odczytu, możesz tu tylko czytać, ale nie pisać."

   ;; schnaq progress bar related stuff
   :discussion.progress/days-left "Pozostało %s dni"
   :discussion.progress/unlimited "Nieograniczony otwarty"
   :discussion.progress/end "Koniec dyskusji"
   :discussion.progress/ends "Kończy się %s"
   :discussion.progress/ends-not "Nie kończy się"
   :discussion.progress.creation/heading "Ogranicz czas trwania dyskusji"
   :discussion.progress.creation/label "Koniec w dniach"
   :discussion.progress.creation/button-limit "%s Dni"
   :discussion.progress.creation/button-unlimited "Bez ograniczeń"

   ;; Conversion-Edit-Funnel
   :discussion.anonymous-edit.modal/title "Zaloguj się, aby edytować"
   :discussion.anonymous-edit.modal/explain "Aby zapobiec nadużywaniu anonimowych postów, musisz się zalogować, aby móc je edytować."
   :discussion.anonymous-edit.modal/persuade "Posty, które ostatnio utworzyłeś w tej przeglądarce, zostaną przekonwertowane automatycznie."
   :discussion.anonymous-edit.modal/cta "Zaloguj się / Zarejestruj się"

   :discussion.anonymous-labels.modal/title "Zaloguj się, aby edytować etykiety"
   :discussion.anonymous-labels.modal/explain "Aby uniknąć nadużywania anonimowych postów, musisz się zalogować, aby móc edytować labensa."
   :discussion.anonymous-labels.modal/cta "Zaloguj się / Zarejestruj się"

   ;; Conversion-Delete-Funnel
   :discussion.anonymous-delete.modal/title "Zaloguj się, aby usunąć swój post"
   :discussion.anonymous-delete.modal/explain "Aby uniknąć nadużywania anonimowych postów, musisz zarejestrować się w celu ich usunięcia."
   :discussion.anonymous-delete.modal/persuade "Posty, które ostatnio utworzyłeś w tej przeglądarce, zostaną dodane do Twojego konta."
   :discussion.anonymous-delete.modal/cta "Zaloguj się / Zarejestruj się"

   ;; Beta Only Funnel
   :beta.modal/title "Funkcja beta"
   :beta.modal/explain "Jest to funkcja testowa. Aby z niego korzystać, musisz być beta testerem."
   :beta.modal/persuade "Napisz do nas e-mail, jeśli chciałbyś zostać jednym z beta testerów.."
   :beta.modal/cta "Wyślij zapytanie"
   :beta.modal.login/intro "Jesteś już beta testerem?"
   :beta.modal.login/button "Następnie zarejestruj się"
   :page.beta.modal/cta "Jeśli jesteś zainteresowany zostaniem beta testerem, wyślij do nas e-mail na adres"

   ;; Press Kit
   :press-kit/heading "Prasa i media"
   :press-kit/subheading "Chętnie udzielamy wywiadów i artykułów!"
   :press-kit.intro/heading "Dziękujemy za zainteresowanie schnaq!"
   :press-kit.intro/lead "Proszę poświęcić chwilę na zapoznanie się z naszymi wytycznymi dotyczącymi marki. Jeśli mają Państwo pytania prasowe lub chcieliby Państwo o nas napisać, prosimy o kontakt mailowy: presse@schnaq.com. Chętnie z Tobą porozmawiamy!"
   :press-kit.spelling/heading "Poprawna pisownia i wymowa"
   :press-kit.spelling/content-1 "Nasz produkt nazywa się"
   :press-kit.spelling/content-2 "(mówione: [ˈʃnak]) i jest pisany przez \"q\". Wymawia się ją z miękkim \"sch\", analogicznie do północnoniemieckiego \"schnacken\". Z wyjątkiem początku zdania, schnaq powinien być pisany małą literą. Płeć gramatyczna słowa schnaq jest męska, więc oznacza ono, na przykład, \"der schnaq\" lub \"stworzyć schnaq\"."
   :press-kit.not-to-do/heading "Proszę zwrócić uwagę na następujące kwestie"
   :press-kit.not-to-do/bullet-1 "Nie używaj żadnych innych zdjęć, ilustracji, treści lub innych zasobów z tej domeny bez zgody."
   :press-kit.not-to-do/bullet-2 "Unikaj wyświetlania tych grafik w sposób, który sugeruje związek, przynależność lub poparcie przez schnaq. Jeśli nie jesteś pewien, skontaktuj się z nami."
   :press-kit.not-to-do/bullet-3 "Nie należy używać tych grafik jako części nazwy własnego produktu, firmy lub usługi."
   :press-kit.not-to-do/bullet-4 "Prosimy nie zmieniać tych grafik w jakikolwiek sposób lub łączyć je z innymi grafikami bez naszej pisemnej zgody."
   :press-kit.materials/heading "Materiały"
   :press-kit.materials/fact-sheet "Arkusz informacyjny"
   :press-kit.materials/logos "Loga"
   :press-kit.materials/product "Zdjęcia produktów"
   :press-kit.materials/team "Zdjęcia zespołu"
   :press-kit.materials/download "Pobierz"
   :press-kit.about-us/heading "Dalsze informacje"
   :press-kit.about-us/body "Więcej informacji o naszych założycielach, publikacjach naukowych i innych wystąpieniach w gazetach i mediach można znaleźć na następujących stronach:"

   ;; Publications
   :publications/heading "Publikacje i artykuły"
   :publications/subheading "Nauka stojąca za schnaqiem"
   :publications.primer/heading "Od nauki do praktyki"
   :publications.primer/body "Oprogramowanie, które tworzymy, opiera się nie tylko na doświadczeniu, ale także na wieloletnich badaniach w dziedzinie dyskusji i komunikacji. Tutaj znajdą Państwo artykuły naukowe, artykuły prasowe i inne publikacje, które pochodzą od naszego zespołu lub zostały opracowane we współpracy z naszym zespołem."

   :publications.perspective-daily/summary "Artykuł o naszych badaniach w Perspective Daily. Skup się na ustrukturyzowanej dyskusji"
   :publications.salto/summary "Wywiad z naszymi założycielami dr Christianem Meterem i dr Alexandrem Schneiderem o dyskusjach w internecie, trollach i sposobach radzenia sobie z nimi."
   :publications.dissertation-alex/summary "Praca doktorska dr Alexandra Schneidera dotyczy pytania, czy możliwe jest prowadzenie ustrukturyzowanych dyskusji w Internecie przy użyciu systemów zdecentralizowanych."
   :publications.dissertation-christian/summary "W rozprawie dr Christiana Meter'a, kilka nowych metod i podejść jest badanych w celu umożliwienia prowadzenia ustrukturyzowanych dyskusji w Internecie."
   :publications.structure-or-content/summary "Niniejszy artykuł analizuje, czy Pagerank jako algorytm może składać wiarygodne oświadczenia na temat trafności argumentów i jak jego wydajność wypada w porównaniu z nowszymi algorytmami."
   :publications.overview-paper/summary "Prezentacja szerokiego wachlarza metod usprawniania rzeczywistych dyskusji w Internecie."
   :publications.dbas/summary "Opis formalnego prototypu dla argumentacji online opartej na dialogu, wraz z oceną."
   :publications.dbas-politics/summary "Prezentacja koncepcji dialogowych dyskusji internetowych dla laików."
   :publications.eden/summary "Prezentacja pakietu oprogramowania umożliwiającego działanie zdecentralizowanych serwerów dających użytkownikom dostęp do internetowych systemów dyskusyjnych."
   :publications.jebediah/summary "Artykuł demonstruje bota społecznościowego opartego na silniku Dialogflow firmy Google. Bot jest w stanie komunikować się ze swoimi użytkownikami w sieciach społecznościowych w sposób oparty na dialogu."
   :publications.dbas-experiment/summary "W eksperymencie terenowym z udziałem ponad 100 osób testujących, artykuł bada, jak dobrze system argumentacji oparty na dialogu może być używany przez laików."
   :publications.reusable-statements/summary "Autorzy badają ideę uczynienia argumentów online i ich wzajemnych powiązań użytecznymi i możliwymi do ponownego wykorzystania jako zasoby."
   :publications.discuss/summary "Jeśli ustrukturyzowane dyskusje są możliwe dzięki oprogramowaniu, to czy jest również możliwe, aby te dyskusje odbywały się w dowolnym kontekście sieciowym? To jest pytanie, które badają autorzy."
   :publications.kind/article "Artykuł"
   :publications.kind/dissertation "Dysertacja"
   :publications.kind/interview "Wywiad"
   :publications.kind/newspaper-article "Artykuł w gazecie"
   :publications.kind/paper "Paper (angielski)"
   :publications.kind/short-paper "Shortpaper (angielski)"

   ;; Privacy Page
   :privacy/heading "Co się dzieje z Twoimi danymi?"
   :privacy/subheading "Chętnie wyjaśnimy!"
   :privacy/open-settings "Sprawdź ustawienia"
   :privacy.made-in-germany/lead "Procedura zgodna z wymogami UE"
   :privacy.made-in-germany/title "Ochrona danych jest dla nas ważna!"
   :privacy.made-in-germany/body
   [:<>
    [:p "Zespół programistów schnaq składa się z informatyków, którzy są zmęczeni tym, że dane nie są traktowane z należytą uwagą. Dlatego szczególną wagę przykładamy do zgodności z GDPR i bezpiecznego przechowywania wszystkich danych na serwerach firmy Hetzner w Niemczech. Bez wymiany danych z innymi firmami, bez leniwych kompromisów!"]
    [:p "Jeśli nadal nie wiesz, jak obchodzimy się z Twoimi danymi, skontaktuj się z nami! Przejrzystość i jasność w zakresie danych osobowych jest dla nas naprawdę ważna, dlatego wyjaśniamy, co dzieje się z danymi do ostatniego szczegółu."]]
   :privacy.personal-data/lead "Jakie dane są zbierane?"
   :privacy.personal-data/title "Dane osobowe"
   :privacy.personal-data/body
   [:<>
    [:p "Domyślnie gromadzone są tylko dane niezbędne z technicznego punktu widzenia. Nie dochodzi do przetwarzania danych osobowych, a Państwa zachowanie na naszej stronie internetowej jest analizowane wyłącznie w sposób anonimowy."]
    [:p "Twoje zachowanie użytkownika jest rejestrowane przez Matomo i przechowywane na naszych serwerach w Niemczech. Matomo jest darmową i samodzielnie utrzymywaną alternatywą dla komercyjnych dostawców. Nie przekazujemy za jego pomocą żadnych danych osobom trzecim."]]
   :privacy.localstorage/lead "Jakie dane wysyłam do serwerów?"
   :privacy.localstorage/title "Wymiana danych"
   :privacy.localstorage/body
   [:<>
    [:p "schnaq może obejść się bez kont. W ten sposób żadne Twoje dane nie są przechowywane na naszych serwerach. Większość interakcji odbywa się poprzez udostępnione linki. Kiedy klikasz na link do schnaq, część linku (hash) jest przechowywana w Twojej przeglądarce (w LocalStorage). Jeśli następnie odwiedzisz schnaq ponownie, Twoja przeglądarka wysyła ten hash z powrotem do nas i w ten sposób uzyskujesz dostęp do schnaq ponownie. Alternatywnie można zlecić przesłanie linków dostępu pocztą elektroniczną i w ten sposób zachować wszystkie dane niezbędne do pracy we własnych rękach."]
    [:p "W przeciwieństwie do konwencjonalnych plików cookie, używamy LocalStorage, który oczywiście odsyła nam tylko te dane, które są naprawdę niezbędne. Sprawdź sam, jakie to są dane, klikając na przycisk."]]
   :privacy.localstorage/show-data "Pokaż swoje dane"
   :privacy.localstorage.notification/title "Twoja przeglądarka zapisała te dane"
   :privacy.localstorage.notification/body "Uwaga: \"Kryptyczne\" ciągi są kodami dostępu do schnaqs."
   :privacy.localstorage.notification/confirmation "Czy naprawdę chcesz usunąć swoje dane?"
   :privacy.localstorage.notification/delete-button "Usuń dane"

   :privacy.data-processing.anonymous/lead "Co dzieje się z Twoimi składkami?"
   :privacy.data-processing.anonymous/title "Przetwarzanie danych w celu uzyskania anonimowego dostępu"
   :privacy.data-processing.anonymous/body [:<> [:p "Wpisane przez Ciebie wypowiedzi w połączeniu z wybraną przez Ciebie nazwą użytkownika przechowujemy na naszym serwerze i nie przekazujemy ich osobom trzecim. Jeśli nie podasz nazwy użytkownika, jako autor zostanie wpisany \"Anonimowy\". Wypowiedzi, które napisałeś, nie są ze sobą powiązane. Ponieważ nie pamiętamy, od kogo pochodzi wkład, nie ma możliwości edycji wkładu. Żadne dane osobowe, takie jak adres przeglądarki lub IP, nie zostaną połączone z Twoimi wypowiedziami."]
                                            [:p "Posty w publicznych schnaqs mogą być oglądane przez wszystkich użytkowników. Posty w prywatnych schnaqs mogą być oglądane tylko przez osoby, które mają link do dyskusji. Administratorzy schnaq mają możliwość usuwania postów."]]
   :privacy.data-processing.registered/lead "A kiedy jestem teraz zalogowany?"
   :privacy.data-processing.registered/title "Przetwarzanie danych dla zarejestrowanych użytkowników"
   :privacy.data-processing.registered/body
   [:<> [:p "Jeśli zdecydujesz się na rejestrację, Twój adres e-mail i imię zostaną zapisane. To pozwala nam spersonalizować Twoje doświadczenia ze schnaq i wyświetlać Twoje imię, gdy zapisujesz post. Adres e-mail jest potrzebny między innymi do powiadomień, abyś był informowany, gdy pojawią się nowe posty dla Ciebie."]
    [:p "Gdy logują się Państwo za pośrednictwem zewnętrznego dostawcy, takiego jak LinkedIn, LinkedIn otrzymuje od Państwa prośbę o przekazanie nam wyświetlanych informacji, które następnie przechowujemy. Jeśli zalogujesz się ponownie, LinkedIn również otrzyma kolejną prośbę. Jeśli chcesz tego uniknąć, po prostu załóż konto bezpośrednio u nas."]
    [:p "Dodatkowo, przechowujemy huby i schnaqs, do których masz dostęp na swoim koncie. Oznacza to, że możesz również zalogować się na swoim smartfonie lub innym urządzeniu i mieć dostęp do wszystkich swoich schnaqów."]
    [:p "Teraz możliwe jest również korzystanie z zaawansowanych funkcji, takich jak edytowanie postów, ponieważ masz teraz tożsamość na naszej platformie 👍"]
    [:p "W każdej chwili mogą Państwo skontaktować się z nami i poprosić o dostęp do swoich danych lub ich usunięcie."]]

   :privacy.link-to-privacy/lead "Więcej informacji można znaleźć w naszych szczegółowych"
   :privacy/note "Polityka prywatności"

   :privacy.extended/heading "Polityka prywatności"
   :privacy.extended/subheading "Jesteśmy zgodni z GDPR"
   :privacy.extended.intro/title "Ogólne informacje na temat przetwarzania danych"
   :privacy.extended.intro/body
   [:<>
    [:p "Zasadniczo przetwarzamy dane osobowe tylko w takim zakresie, w jakim jest to konieczne do zapewnienia funkcjonowania strony internetowej i naszych treści. Dane osobowe są regularnie przetwarzane tylko za zgodą użytkownika."]
    [:p "O ile do przetwarzania danych osobowych wymagana jest zgoda, jako podstawa prawna służy art. 6 ust. 1 lit. a) ogólnego rozporządzenia UE o ochronie danych (GDPR).\nJeśli przetwarzanie danych jest konieczne do ochrony uzasadnionego interesu z naszej strony lub ze strony osoby trzeciej, a Państwa interesy, prawa podstawowe i wolności nie przeważają nad pierwszym wymienionym interesem, art. 6 (1) lit. f GDPR służy jako podstawa prawna przetwarzania danych."]
    [:p "Dane osobowe są usuwane, gdy tylko przestaje obowiązywać cel ich przechowywania. Przechowywanie danych może mieć miejsce również wtedy, gdy zostało to przewidziane przez europejskiego lub krajowego ustawodawcę w rozporządzeniach unijnych, ustawach lub innych przepisach, którym podlegamy. Dane zostaną również usunięte, jeśli upłynie okres przechowywania przewidziany przez wyżej wymienione normy."]]
   :privacy.extended.logfiles/title "Udostępnianie strony internetowej i tworzenie plików dziennika systemowego"
   :privacy.extended.logfiles/body
   [:<>
    [:p "Przy każdym wejściu na naszą stronę internetową nasz system automatycznie pobiera dane i informacje (typ / wersja używanej przeglądarki, system operacyjny, adres IP, data i godzina dostępu, strony internetowe, z których uzyskano dostęp do naszej strony internetowej, strony internetowe, do których uzyskano dostęp za pośrednictwem naszej strony internetowej) z systemu komputerowego komputera uzyskującego dostęp. Dane te są zapisywane w plikach logów naszego systemu. Dane te nie są przechowywane razem z innymi danymi osobowymi użytkownika. Podstawą prawną dla tymczasowego przechowywania danych i plików dziennika jest art. 6 ust. 1 lit. f GDPR."]
    [:p "Tymczasowe zapisanie adresu IP przez system jest konieczne, aby umożliwić dostarczenie strony internetowej do komputera użytkownika. W tym celu adres IP musi pozostać zapisany na czas trwania sesji. Zapisywanie w plikach dziennika odbywa się w celu zapewnienia funkcjonalności strony internetowej. Ponadto wykorzystujemy te dane do optymalizacji strony internetowej oraz do zapewnienia bezpieczeństwa naszych systemów informatycznych. Cele te stanowią również nasz uzasadniony interes w przetwarzaniu danych zgodnie z art. 6 ust. 1 lit. f GDPR."]
    [:p "Dane są usuwane, gdy tylko nie są już potrzebne do osiągnięcia celu, dla którego zostały zebrane. W przypadku zbierania danych w celu udostępnienia strony internetowej, ma to miejsce po zakończeniu danej sesji. W przypadku przechowywania danych w plikach dziennika następuje to najpóźniej po siedmiu dniach. Możliwe jest przechowywanie po upływie tego okresu. W takim przypadku adresy IP użytkowników są usuwane lub anonimizowane."]
    [:p "Gromadzenie danych w celu udostępnienia strony internetowej i zapisywanie danych w plikach dziennika jest absolutnie konieczne do funkcjonowania strony internetowej. W związku z tym nie ma możliwości wniesienia sprzeciwu."]]
   :privacy.extended.cookies/title "Cookies"
   :privacy.extended.cookies/body
   [:<>
    [:p "Na naszej stronie internetowej używamy tzw. cookies. Cookies to pakiety danych, które Państwa przeglądarka przechowuje w Państwa urządzeniu końcowym na nasze polecenie. Rozróżnia się dwa rodzaje plików cookies: tymczasowe, tzw. sesyjne (session cookies) oraz stałe (persistent cookies)."]
    [:p "Pliki cookie sesji są automatycznie usuwane po zamknięciu przeglądarki. Przechowują one tak zwany identyfikator sesji, dzięki któremu różne żądania z przeglądarki mogą być przypisane do wspólnej sesji. Pozwala to na rozpoznanie Państwa komputera, gdy powracają Państwo na naszą stronę internetową. Użycie plików cookie sesji jest konieczne, abyśmy mogli udostępnić Państwu stronę internetową. Podstawą prawną do przetwarzania Państwa danych osobowych za pomocą cookies sesyjnych jest art. 6 ust. 1 lit. f GDPR."]
    [:p "Trwałe pliki cookie są automatycznie usuwane po upływie określonego czasu, który może być różny w zależności od pliku cookie. Te pliki cookie pozostają na Państwa urządzeniu końcowym przez określony czas i są zazwyczaj wykorzystywane do rozpoznania Państwa przy ponownej wizycie na naszej stronie internetowej. Stosowanie trwałych plików cookie na naszej stronie internetowej opiera się na podstawie prawnej art. 6 ust. 1 lit. f GDPR."]
    [:p "Mogą Państwo ustawić swoją przeglądarkę internetową tak, aby nasze pliki cookie nie mogły być zapisywane na Państwa urządzeniu końcowym lub aby pliki cookie, które zostały już zapisane, były usuwane. Jeśli nie akceptują Państwo plików cookie, może to prowadzić do ograniczeń w funkcjonowaniu stron internetowych."]
    [:p "W szczególności, mamy następujące rodzaje plików cookie:"]
    [:ul
     [:li "CSRF token (cookie sesyjne), który zabezpiecza formularz kontaktowy przed nieobserwowanym przesłaniem treści. Jest to losowy układ znaków, który jest używany tylko do wysyłania formularza. Ten plik cookie jest usuwany po opuszczeniu naszej strony internetowej. Ten mechanizm ochrony jest zgodny z aktualnymi standardami bezpieczeństwa i może na przykład "
      [:a {:href "https://de.wikipedia.org/wiki/Cross-Site-Request-Forgery"}
       "tutaj"]
      " należy prowadzić dalsze badania."]
     [:li "Plik cookie logowania (persistent cookie), który rozpoznaje Cię jako użytkownika, z którym się zalogowałeś. Po 14 dniach plik cookie wygasa i jest usuwany. Jeśli usuniesz ten plik cookie, będziesz musiał zalogować się ponownie przy następnej wizycie na stronie. Możesz znaleźć nasz serwer uwierzytelniający tutaj: https://auth.schnaq.com"]]
    [:p "Wszystkie używane przez nas pliki cookie generują losowe ciągi znaków, które są używane do dopasowania odpowiadających im ciągów znaków na naszym serwerze."]]

   :privacy.extended.personal-data/title "Dane osobowe"
   :privacy.extended.personal-data/body
   [:<>
    [:h4 "Używanie schnaq bez kont użytkowników"]
    [:p "Jeśli używasz schnaq bez rejestracji, jesteś tak zwanym \"Anonimowym Użytkownikiem\". Oprócz danych wymaganych do działania serwera, zapisywane są tylko Twoje wypowiedzi i opcjonalnie wybrane przez Ciebie imię. Kiedy wkład jest zapisywany, ten ciąg znaków jest luźno zapisywany razem z wkładem. Nie ma przypisania do tożsamości. Jeśli ktoś o tym samym imieniu uczestniczy w jakimś schnaq'u, wkłady wyglądają dla świata zewnętrznego tak, jakby pochodziły od tej samej osoby."]
    [:p "Przesyłając swój wkład, wyrażasz zgodę na jego przechowywanie. Ponieważ nie jesteśmy w stanie ustalić autora tego wkładu, nie masz prawa go usunąć, ponieważ nie ma dowodu na jego autorstwo."]
    [:h4 "Używanie schnaq jako zarejestrowany użytkownik"]
    [:p "Podczas rejestracji zapisywany jest Twój adres e-mail oraz imię i nazwisko. Są one niezbędne do działania schnaq, zbieranie danych odbywa się zgodnie z art. 6 ust. 1 lit. f GDPR. Rejestracja jest opcjonalna dla normalnego działania schnaq. Adres e-mail umożliwia automatyczne powiadamianie o nowych wpłatach. Wraz z nazwiskami, Twój wkład jest wyświetlany razem na interfejsie schnaq. Inne przynależności, na przykład do hubów lub innych schnaqs, są również wizualnie wyświetlane."]
    [:p "Dane te są przechowywane na naszych własnych serwerach i nie są przekazywane osobom trzecim."]
    [:p "Istnieją sposoby na rozbudowę własnego profilu użytkownika. Obejmują one na przykład możliwość załadowania własnego, opcjonalnego zdjęcia profilowego. To zdjęcie profilowe jest następnie wyświetlane jako Twój awatar i jest prezentowane zawsze, gdy pojawia się Twoje konto użytkownika, na przykład gdy ludzie patrzą na Twoje posty."]
    [:h4 "Wkład tekstowy"]
    [:p "Teksty muszą pochodzić od Ciebie i nie mogą naruszać żadnych praw autorskich. Nadesłane teksty nie będą przekazywane osobom trzecim. Wewnętrznie, Twój wkład może być wykorzystany do dalszej oceny naukowej i szkolenia własnych sieci neuronowych. Nigdy nie utracisz swojego autorstwa tych wkładów. Jest to wykorzystywane np. do automatycznego obliczania zestawień i statystyk generowanych przez maszyny. Te podsumowania i statystyki są przeznaczone do oceny Twojego schnaq i nie będą przekazywane osobom trzecim."]]
   :privacy.extended.matomo/title "Analiza stron internetowych za pomocą Matomo (dawniej PIWIK)"
   :privacy.extended.matomo/body
   [:<>
    [:h4 "Opis i zakres przetwarzania danych"]
    [:p "Na naszej stronie internetowej używamy oprogramowania Matomo (dawniej PIWIK), aby analizować wykorzystanie naszej obecności w Internecie. Interesuje nas na przykład, jakie strony są często odwiedzane i czy używane są smartfony, tablety lub komputery z dużymi ekranami. Oprogramowanie nie ustawia plików cookie i nie tworzy profilu odwiedzających. W przypadku wejścia na poszczególne strony naszej witryny internetowej zapisywane są następujące dane:"]
    [:ol
     [:li "Dwa bajty adresu IP systemu wywołującego"]
     [:li "Dostępna strona internetowa"]
     [:li "Strona internetowa, z której nastąpiło wejście na naszą stronę (referrer)"]
     [:li "Podstrony, które są dostępne z danej strony"]
     [:li "Długość pobytu na stronie"]
     [:li "Częstotliwość odwiedzin strony internetowej"]]
    [:p "Matomo jest ustawione w taki sposób, że adresy IP nie są zapisywane w całości, ale dwa bajty adresu IP są maskowane (np.: 192.168.xxx.xxx). W ten sposób nie jest już możliwe przypisanie skróconego adresu IP do komputera wywołującego."]
    [:p "Matomo jest używane wyłącznie na serwerach schnaq. Dane osobowe użytkowników są przechowywane tylko tam. Dane te nie są przekazywane osobom trzecim."]
    [:h4 "Cel przetwarzania danych"]
    [:p "Przetwarzanie zanonimizowanych danych użytkowników umożliwia nam analizę korzystania z naszej strony internetowej. Poprzez analizę uzyskanych danych jesteśmy w stanie opracować informacje na temat korzystania z poszczególnych elementów naszej strony internetowej. Pomaga nam to w ciągłym ulepszaniu naszych usług i ich przyjazności dla użytkownika. Dzięki anonimizacji adresu IP interes użytkownika w zakresie ochrony jego danych osobowych zostaje odpowiednio uwzględniony."]
    [:p "Nie są tworzone żadne profile, które dawałyby nam głębszy wgląd w zachowania użytkowników. Ocena jest wyłącznie anonimowa i zagregowana, tak aby nie można było wyciągać żadnych wniosków na temat poszczególnych osób."]
    [:p "Korzystanie z Matomo na naszej stronie internetowej opiera się na podstawie prawnej Art. 6 ust. 1 lit. f GDPR."]]
   :privacy.extended.facebook-pixel/title "Facebook Pixel"
   :privacy.extended.facebook-pixel/body
   [:<>
    [:p "Na naszej stronie internetowej używamy piksela Facebooka z Facebooka. Zaimplementowaliśmy kod na naszej stronie internetowej, aby to zrobić. Piksel Facebooka to wycinek kodu JavaScript, który ładuje zbiór funkcji umożliwiających Facebookowi śledzenie działań użytkownika, jeśli wszedł on na naszą stronę za pośrednictwem reklam Facebooka. Na przykład, gdy kupujesz produkt na naszej stronie internetowej, uruchamiany jest piksel Facebooka, który zapisuje Twoje działania na naszej stronie internetowej w jednym lub kilku plikach cookie. Te pliki cookie umożliwiają Facebookowi dopasowanie danych użytkownika (dane klienta, takie jak adres IP, identyfikator użytkownika) do danych konta na Facebooku. Następnie Facebook ponownie usuwa te dane. Zebrane dane są anonimowe i niewidoczne dla nas i mogą być wykorzystywane tylko w kontekście zamieszczania reklam. Jeśli jesteś użytkownikiem Facebooka i jesteś zalogowany, Twoja wizyta na naszej stronie jest automatycznie przypisywana do Twojego konta użytkownika na Facebooku."]
    [:p "Chcemy pokazywać nasze usługi i produkty tylko tym osobom, które są nimi naprawdę zainteresowane. Za pomocą pikseli Facebooka nasze działania reklamowe mogą być lepiej dopasowane do Państwa życzeń i zainteresowań. W ten sposób użytkownicy Facebooka (o ile zezwolili na spersonalizowaną reklamę) widzą odpowiednie reklamy. Ponadto Facebook wykorzystuje zebrane dane do celów analitycznych i własnych reklam."]
    [:p "O ile jesteś zalogowany na Facebooku, możesz samodzielnie zmienić swoje ustawienia dotyczące reklam pod adresem https://www.facebook.com/ads/preferences/?entry_product=ad_settings_screen."]
    [:p "Pragniemy zwrócić uwagę, że zgodnie z opinią Europejskiego Trybunału Sprawiedliwości, nie istnieje obecnie odpowiedni poziom ochrony danych przekazywanych do USA. Przetwarzanie danych odbywa się zasadniczo przez Facebook Pixel. Może to spowodować, że dane nie będą przetwarzane i przechowywane w sposób anonimowy. Ponadto organy rządowe USA mogą mieć dostęp do indywidualnych danych. Może się również zdarzyć, że dane te zostaną powiązane z danymi z innych serwisów Facebooka, w których użytkownik posiada konto."]
    [:p "Jeśli chcesz dowiedzieć się więcej o polityce prywatności Facebooka, zalecamy zapoznanie się z polityką prywatności firmy pod adresem https://www.facebook.com/policy.php."]
    [:p [:small "Źródło: Stworzone za pomocą generatora prywatności z AdSimple"]]]
   :privacy.extended.facebook-pixel-addition/title "Piksel Facebooka: Automatyczne, zaawansowane dopasowanie"
   :privacy.extended.facebook-pixel-addition/body
   [:<>
    [:p "W ramach funkcji Piksela Facebooka włączyliśmy również automatyczne dopasowywanie zaawansowane. Ta funkcja piksela umożliwia nam wysyłanie do Facebooka haseł wiadomości e-mail, imienia i nazwiska, płci, miasta, stanu, kodu pocztowego i daty urodzenia lub numeru telefonu jako dodatkowych informacji, jeśli użytkownik udostępnił nam te dane. Dzięki tej aktywacji możemy jeszcze dokładniej dopasować kampanie reklamowe na Facebooku do osób, które są zainteresowane naszymi usługami lub produktami."]
    [:small "Źródło: Stworzone za pomocą generatora prywatności z AdSimple"]]
   :privacy.extended.rights-of-the-affected/title "Prawa osób, których dane dotyczą"
   :privacy.extended.rights-of-the-affected/body
   [:<>
    [:p "Jeśli Państwa dane osobowe są przetwarzane, jesteście Państwo podmiotem danych w rozumieniu rozporządzenia o ochronie danych osobowych. GDPR i przysługują Państwu prawa opisane poniżej. Prosimy o przesłanie prośby, najlepiej pocztą elektroniczną, do wyżej wymienionego administratora danych."]
    [:p [:strong "Informacje:"]
     " W każdej chwili mają Państwo prawo do bezpłatnego otrzymania od nas informacji i potwierdzenia o przechowywanych danych osobowych oraz kopii tych informacji."]
    [:p [:strong "Korekta:"]
     " Mają Państwo prawo do sprostowania i/lub uzupełnienia, jeśli przetwarzane dane osobowe dotyczące Państwa są niedokładne lub niekompletne."]
    [:p [:strong "Ograniczenie przetwarzania danych:"]
     " Masz prawo zażądać ograniczenia przetwarzania danych, jeśli spełniony jest jeden z następujących warunków:"]
    [:ul
     [:li "Użytkownik kwestionuje prawidłowość danych osobowych przez okres czasu, który umożliwia nam sprawdzenie prawidłowości danych osobowych."]
     [:li "Przetwarzanie jest niezgodne z prawem, odmawiają Państwo usunięcia danych osobowych, a zamiast tego żądają ograniczenia ich wykorzystywania."]
     [:li "Nie potrzebujemy już tych danych osobowych do celów przetwarzania, ale są one potrzebne do dochodzenia, wykonywania lub obrony roszczeń prawnych."]
     [:li "Zgłosili Państwo sprzeciw wobec przetwarzania danych zgodnie z art. 21 ust. 1 GDPR i nie jest jeszcze jasne, czy nasze uzasadnione powody przeważają nad Państwa. "]]
    [:p [:strong "Usunięcie:"]
     " Mają Państwo prawo do bezzwłocznego usunięcia dotyczących Państwa danych osobowych, jeżeli zachodzi jedna z poniższych przyczyn i o ile przetwarzanie nie jest konieczne:"]
    [:ul
     [:li "Dane osobowe były gromadzone lub w inny sposób przetwarzane do celów, do których nie są już potrzebne. "]
     [:li "Wycofujesz swoją zgodę, na której opierało się przetwarzanie danych i nie ma innej podstawy prawnej do przetwarzania danych. "]
     [:li "Użytkownik sprzeciwia się przetwarzaniu danych zgodnie z art. 21 ust. 1 GDPR i nie istnieją żadne nadrzędne uzasadnione podstawy do przetwarzania danych lub użytkownik sprzeciwia się przetwarzaniu danych zgodnie z art. 21 ust. 2 GDPR. "]
     [:li "Dane osobowe były przetwarzane niezgodnie z prawem."]
     [:li "Usunięcie danych osobowych jest niezbędne do wypełnienia obowiązku prawnego wynikającego z prawa Unii lub państwa członkowskiego, któremu podlegamy."]
     [:li "Dane osobowe zostały zebrane w związku z usługami społeczeństwa informacyjnego oferowanymi zgodnie z art. 8 ust. 1 GDPR."]]
    [:p [:strong "Możliwość przenoszenia danych:"]
     " Mają Państwo prawo do otrzymania dotyczących Państwa danych osobowych, które dostarczyli Państwo administratorowi w ustrukturyzowanym, powszechnie używanym i nadającym się do odczytu maszynowego formacie. Mają Państwo również prawo do przekazania tych danych innemu administratorowi bez przeszkód ze strony administratora, któremu dane osobowe zostały przekazane. Korzystając z tego prawa, mają Państwo również prawo do tego, aby odnoszące się do Państwa dane osobowe zostały przekazane bezpośrednio przez nas innemu administratorowi, o ile jest to technicznie wykonalne. Nie może to naruszać swobód i praw innych osób."]
    [:p [:strong "Opozycja:"]
     " W każdej chwili mają Państwo prawo sprzeciwić się przetwarzaniu Państwa danych osobowych na podstawie art. 6 (1) lit. f GDPR. W przypadku wniesienia sprzeciwu nie będziemy już przetwarzać danych osobowych, chyba że będziemy w stanie wykazać ważne prawnie uzasadnione podstawy do przetwarzania, które są nadrzędne wobec Państwa interesów, praw i wolności, lub przetwarzanie służy dochodzeniu, wykonywaniu lub obronie roszczeń prawnych."]
    [:p [:strong "Cofnięcie zgody:"]
     " Zgodnie z prawem o ochronie danych osobowych mają Państwo prawo w każdej chwili odwołać swoją deklarację zgody. Cofnięcie zgody nie wpływa na zgodność z prawem przetwarzania, którego dokonano na podstawie zgody do momentu jej cofnięcia."]]
   :privacy.extended.right-to-complain/title "Prawo do złożenia skargi do organu nadzorczego"
   :privacy.extended.right-to-complain/body
   [:<>
    [:p "Bez uszczerbku dla wszelkich innych administracyjnych lub sądowych środków odwoławczych, użytkownik ma prawo do złożenia skargi do organu nadzorczego, w szczególności w państwie członkowskim zamieszkania, jeśli uważa, że przetwarzanie danych osobowych dotyczących użytkownika narusza GDPR.\nOrganem nadzorczym w zakresie ochrony danych osobowych odpowiedzialnym za operatora tej strony jest:"]
    [:p "Ochrony Danych i Wolności Informacji NRW, Kavalleriestr. 2-4, 40102 Düsseldorf, Tel.: +49211/38424-0, E-Mail: poststelle{at}ldi.nrw.de"]]
   :privacy.extended.hosting/title "Hosting strony internetowej"
   :privacy.extended.hosting/body
   [:<>
    [:p "Strona schnaq jest umieszczona na serwerach firmy Hetzner Online GmbH w Niemczech. Więcej informacji na ten temat można znaleźć na stronie internetowej Hetzner Online GmbH."]
    [:h4 "Zawarcie umowy dotyczącej przetwarzania danych na zlecenie (umowa AV)"]
    [:p "Zawarliśmy z firmą Hetzner Online GmbH umowę AV, która chroni naszych klientów i zobowiązuje firmę Hetzner do nieprzekazywania zgromadzonych danych osobom trzecim.."]]
   :privacy.extended.responsible/title "Osoba odpowiedzialna"
   :privacy.extended.responsible/body
   [:<>
    [:p
     "schnaq (nie ustalono)" [:br]
     "reprezentowani przez Christian Meter, Alexander Schneider und Michael Birkhoff" [:br]
     "Speditionsstraße 15A" [:br]
     "STARTPLATZ" [:br]
     "40221 Düsseldorf" [:br]
     "Niemcy" [:br]
     (toolbelt/obfuscate-mail "info@schnaq.com")]
    [:p "Prawnie wiążąca jest niemiecka wersja tej strony."]]

   ;; About us
   :about-us.unity/title "Jednostka schnaq"
   :about-us.unity/body [:<> [:p "schnaq przenosi cyfrowe dyskusje w przyszłość. Oferujemy firmom możliwość prowadzenia przejrzystych procesów decyzyjnych, w których cały zespół może być wysłuchany, dzięki czemu mają miejsce równe szanse i zrozumiałe dyskursy. Nasza analityka pomoże Ci zrozumieć, który członek zespołu nie został wystarczająco wysłuchany i powinien zostać uwzględniony. Dzieląc się wiedzą poprzez dyskusje na naszej platformie, zapobiegamy powstawaniu silosów wiedzy i milczącej wiedzy firmowej poprzez udostępnianie jej wszystkim, czy to w formie pisemnej, czy późniejszej komunikacji ustnej."]
                         [:p "Nasz zespół stoi na straży tego, aby każdy głos był słyszalny!"]]

   :about-us.value/title "Nasze wartości"
   :about-us.value/subtitle "Kierujemy się wartościami, które definiują nasze działania i nasze produkty."
   :about-us.honesty/title "Uczciwość"
   :about-us.honesty/body "Skupiamy się na uczciwym i pozbawionym przesady przedstawianiu naszych produktów i ich możliwości. Głęboko wierzymy, że nasze produkty mogą stać za siebie bez żadnej przesady."
   :about-us.collaborate/title "Chęć współpracy"
   :about-us.collaborate/body "Głęboko wierzymy, że razem możemy osiągnąć więcej niż w pojedynkę. Dlatego też lubimy kultywować kulturę współpracy. Zarówno we własnym zespole, jak i z naszymi klientami i partnerami współpracy. Razem możemy tworzyć wielkie rzeczy."
   :about-us.action/title "Działanie"
   :about-us.action/body "Nie podejmujemy decyzji znienacka, ale w oparciu o wszystkie dane, które posiadamy. Ale kiedy po dyskusjach zostanie podjęta decyzja, stoimy za nią razem i wspólnie dążymy do efektywnego działania."
   :about-us.quality/title "Jakość"
   :about-us.quality/body "Jesteśmy dumni z naszej pracy i tego, co tworzymy. Lubimy naszą pracę, postrzegamy ją jako część nas samych i cieszymy się, że łączy ludzi na całym świecie. Dlatego tak ważne jest dla nas, aby nasze produkty były najwyższej możliwej jakości."
   :about-us.diversity/title "Różnorodność"
   :about-us.diversity/body "Każdy człowiek wnosi swoje unikalne spojrzenie na świat. I właśnie dlatego, że wprowadzamy ludzi w kontakt ze sobą, chcemy, aby jak najwięcej z tych perspektyw wpływało na naszą pracę."

   :about-us.numbers/title "schnaq w liczbach"
   :about-us.numbers/research "Lata badań"
   :about-us.numbers/users "Użytkownicy"
   :about-us.numbers/statements "Oświadczenia uporządkowane"
   :about-us.numbers/loc "Linie kodu"

   :about-us.team/title "Zespół w centrum uwagi"
   :about-us.team/alexander "Współzałożyciel - zarządzanie operacyjne"
   :about-us.team/christian "Współzałożyciel - Przywództwo techniczne"
   :about-us.team/mike "Współzałożyciel - Przywództwo w zakresie projektowania produktów"

   :about-us.page/heading "O nas"
   :about-us.page/subheading "Informacje o nas"

   ;; Legal Note
   :legal-note.page/heading "Impressum"
   :legal-note.page/disclaimer "Zastrzeżenie"

   :legal-note.contents/title "Odpowiedzialność za zawartość"
   :legal-note.contents/body "Jako usługodawca jesteśmy odpowiedzialni za własne treści na tych stronach zgodnie z ogólnymi przepisami prawa zgodnie z § 7 ust. 1 niemieckiej ustawy o telemediach (TMG). Zgodnie z §§ 8 do 10 TMG nie jesteśmy jednak zobowiązani jako usługodawca do monitorowania przekazywanych lub zapisywanych informacji osób trzecich lub do badania okoliczności wskazujących na nielegalną działalność. Zobowiązania do usunięcia lub zablokowania wykorzystania informacji zgodnie z ogólnymi przepisami prawa pozostają nienaruszone. Odpowiedzialność w tym zakresie jest jednak możliwa dopiero od momentu, w którym znane jest konkretne naruszenie prawa. Jeśli dowiemy się o takich naruszeniach, natychmiast usuniemy te treści."
   :legal-note.links/title "Odpowiedzialność za linki"
   :legal-note.links/body "Nasza oferta zawiera linki do zewnętrznych stron internetowych osób trzecich, na których treść nie mamy żadnego wpływu. Dlatego nie możemy przejąć żadnej odpowiedzialności za te treści zewnętrzne. Za treść stron, do których odsyłają linki, odpowiada zawsze dany oferent lub operator strony. Strony, do których odsyłają linki, zostały sprawdzone pod kątem ewentualnych naruszeń prawa w momencie umieszczania linków. Nielegalne treści nie były rozpoznawalne w momencie linkowania. Stała kontrola treści stron, do których prowadzą linki, nie jest jednak uzasadniona bez konkretnych przesłanek wskazujących na naruszenie prawa. Jeśli dowiemy się o naruszeniu prawa, natychmiast usuniemy takie linki."
   :legal-note.copyright/title "Copyright"
   :legal-note.copyright/body "Treści i dzieła stworzone przez administratorów stron na tych stronach podlegają niemieckiemu prawu autorskiemu. Powielanie, przetwarzanie, rozpowszechnianie i wszelkiego rodzaju wykorzystywanie wykraczające poza granice prawa autorskiego wymaga pisemnej zgody danego autora lub twórcy. Pobieranie i kopiowanie tej strony jest dozwolone tylko do prywatnego, niekomercyjnego użytku. O ile treści na tej stronie nie zostały stworzone przez operatora, prawa autorskie osób trzecich są respektowane. W szczególności treści osób trzecich są oznaczone jako takie. Jeśli mimo to dowiedzą się Państwo o naruszeniu praw autorskich, prosimy o stosowną informację. Jeśli dowiemy się o jakichkolwiek naruszeniach, natychmiast usuniemy takie treści."
   :legal-note.privacy/title "Polityka prywatności"
   :legal-note.privacy/body "Naszą politykę prywatności można znaleźć tutaj."

   ;; Celebrations
   :celebrations.schnaq-filled/title "🎉 Gratulacje 🎉"
   :celebrations.schnaq-filled/lead "Wypełniłeś nowy schnaq pierwszym oświadczeniem. Jest to pierwszy kamień milowy do udanej dyskusji. 💪"
   :celebrations.schnaq-filled/share-now "Teraz podziel się schnaqiem z zespołem!"
   :celebrations.schnaq-filled/button "Opcje udostępniania"
   :celebrations.first-schnaq-created/title "Stworzyłeś swój pierwszy schnaq 🎈"
   :celebrations.first-schnaq-created/lead "Czy chcesz połączyć swojego schnaqa z kontem? Następnie zarejestruj się za pomocą kilku kliknięć 🚀"

   ;; schnaqs not found
   :schnaqs.not-found/alert-lead "Nie znaleziono schnaqs"
   :schnaqs.not-found/alert-body "Stwórz schnaq lub daj się zaprosić"

   ;; Admin Center
   :schnaq/educate-on-link-text "Podziel się poniższym linkiem ze swoimi kolegami i przyjaciółmi."
   :schnaq/educate-on-link-text-subtitle "Udział w konkursie jest możliwy dla każdego, kto zna link!"
   :schnaq.admin/heading "Centrum Administracyjne"
   :schnaq.admin/subheading "schnaq: \"%s\""
   :schnaq.admin.edit.link/header "Dostęp do Centrum Administracyjnego"
   :schnaq.admin.edit.link/primer "Administracja to praca, pozwól nam sobie pomóc!"
   :schnaq.admin.edit.link/admin "Dostęp do Centrum Administracyjnego za pośrednictwem poczty"
   :schnaq.admin.edit.link/admin-privileges "Edycja i zarządzanie propozycjami"
   :schnaq.admin.edit.link.form/label "Adres e-mail administratora"
   :schnaq.admin.edit.link.form/placeholder "Wprowadź adres e-mail"
   :schnaq.admin.edit.link.form/submit-button "Wyślij link"
   :schnaq.admin.invite/via-link "Rozprowadzić link"
   :schnaq.admin.invite/via-mail "Zaproś przez e-mail"
   :schnaq.admin.edit/administrate "Zarządzaj schnaq"
   :schnaq.export/as-text "Pobierz schnaq jako plik tekstowy"
   :schnaq.admin/tooltip "Zarządzaj schnaq"
   :share-link/copy "Skopiuj link dostępu"
   :share-link/via "Za link"
   :share-access-code/via "Przez kod dostępu"
   :share-access-code/title "Odwiedź schnaq.app"

   :sharing/tooltip "udział sznaq"
   :sharing.modal/title "Podziel się swoim schnaqiem"
   :sharing.modal/lead "Zaproś cały zespół do wypełnienia tego schnaqa wiedzą"
   :sharing.modal/schnaqqi-help "Wypełnij schnaq swoimi pomysłami. Twoim kolegom łatwiej będzie rozpocząć pracę."
   :sharing.modal/qanda-help "Uczestnicy mogą zadawać pytania dotyczące wydarzenia poprzez widok Q&A. Albo bezpośrednio przez link, albo przez kod na www.schnaq.app!"

   ;; Discussion Language
   :discussion/create-argument-action "Dodaj wkład"
   :discussion/add-argument-conclusion-placeholder "Oto, co o tym myślę."
   :discussion/add-premise-supporting "Chciałabym poprzeć oświadczenie"
   :discussion/add-premise-against "Mam powód, by się temu sprzeciwić"
   :discussion/add-premise-neutral "Chciałabym coś dodać"
   :discussion.add.button/support "Dla tego"
   :discussion.add.button/attack "Wobec tego"
   :discussion.add.button/neutral "Neutralny"
   :discussion.add.statement/new "Nowy wkład od Ciebie"
   :discussion.badges/user-overview "Wszyscy uczestnicy"
   :discussion.badges/delete-statement "Usuń post"
   :discussion.badges/posts "Składki"
   :discussion.badges/delete-statement-confirmation "Czy naprawdę chcesz usunąć ten post?"
   :discussion.notification/new-content-title "Nowy wkład!"
   :discussion.notification/new-content-body "Twój post został pomyślnie zapisany.."
   :discussion.badges/edit-statement "edit"
   :discussion.badges/statement-by "z"
   :discussion.badges/new "Nowy"
   :discussion.button/text "Przegląd"

   ;; Q & A
   :qanda/add-question "Wpisz swoje pytanie"
   :qanda.button/text "Q&A"
   :qanda.button/submit "Zadaj pytanie"
   :qanda.state/read-only-warning "Ten schnaq jest tylko do odczytu, nie możesz zadawać żadnych pytań w tej chwili."
   :call-to-qanda/display-code "Kod uczestnictwa:"
   :call-to-qanda/help "Wszystkie opcje udostępniania Twojego schnaqa można znaleźć w prawym górnym pasku nawigacji"

   :schnaqs/header "Przegląd Twojego schnaqs"
   :schnaqs/subheader "Masz dostęp do tych schnaqs"
   :schnaqs/author "Autor"
   :schnaqs/schnaq "schnaq"

   ;; Feedback
   :feedbacks.overview/header "Informacje zwrotne"
   :feedbacks.overview/subheader "Wszystkie przedłożone informacje zwrotne"
   :feedbacks.overview/description "Opis"
   :feedbacks.overview/table-header "Są %d odpowiedzi 🥳!"
   :feedbacks.overview/when? "Kiedy?"
   :feedbacks.overview/contact-name "ze strony"
   :feedbacks.overview/contact-mail "E-mail"
   :feedbacks/button "Informacje zwrotne"
   :feedbacks/screenshot "Zrzut ekranu"
   :feedbacks.modal/primer "Informacja zwrotna jest ważna! Jesteśmy bardzo zadowoleni z
   każdy rodzaj informacji zwrotnej, im bardziej szczera tym lepiej \uD83E\uDD73 Proszę zostawić nam
   zostaw nam komentarz i pomóż nam ulepszyć to oprogramowanie.
   w celu ulepszenia tego oprogramowania. Dziękuję!"
   :feedbacks.modal/contact-name "Twoje imię i nazwisko"
   :feedbacks.modal/contact-mail "Adres e-mail"
   :feedbacks.modal/description "Twoja opinia"
   :feedbacks.modal/optional "Opcjonalnie"
   :feedbacks.modal/screenshot "Wysłać zdjęcie aplikacji?"
   :feedbacks.modal/disclaimer "Twoje dane będą przechowywane wyłącznie na naszych serwerach i i nie będą udostępniane osobom trzecim."
   :feedbacks.notification/title "Dziękujemy za Twoją opinię!"
   :feedbacks.notification/body "Twoja opinia została pomyślnie wysłana do nas 🎉"

   ;; analytics
   :analytics/heading "Analizy"
   :analytics/overall-discussions "Schnaqs stworzył"
   :analytics/user-numbers "Utworzone nazwy użytkowników"
   :analytics/registered-users-numbers "Zarejestrowani użytkownicy"
   :analytics/average-statements-title "Średnia liczba składek na schnaq"
   :analytics/statements-num-title "Liczba oświadczeń"
   :analytics/active-users-num-title "Aktywni użytkownicy (min. 1 wkład)"
   :analytics/statement-lengths-title "Długości wkładów"
   :analytics/statement-types-title "Typy argumentów"
   :analytics/fetch-data-button "Pobierz dane"

   ;; Supporters
   :supporters/heading "Wspierane przez Ministerstwo Gospodarki Kraju Związkowego Nadrenia Północna-Westfalia (Niemcy)"

   ;; Testimonials
   :testimonials/heading "Już przekąszali z nami"
   :testimonials.doctronic/company "doctronic GmbH & Co. KG"
   :testimonials.doctronic/quote "Obserwujemy rozwój schnaq z wielkim zainteresowaniem dla naszego własnego użytku i dla użytku naszych klientów."
   :testimonials.doctronic/author "Ingo Küper, Dyrektor Zarządzający"

   :testimonials.leetdesk/company "Leetdesk – ODYN GmbH"
   :testimonials.leetdesk/quote "Nawet w naszym niewielkim zespole pomocne jest zebranie myśli, aby móc odpowiednio poprowadzić dyskusję. Schnaq umożliwił nam to bardzo dobrze, bardziej efektywne spotkania były rezultatem."
   :testimonials.leetdesk/author "Meiko Tse, Dyrektor Zarządzający"

   :testimonials.hhu/company "Heinrich-Heine-University Düsseldorf"
   :testimonials.bjorn/quote "Do wewnętrznej koordynacji i porozumienia użyliśmy schnaq, aby wszyscy uczestnicy mogli zapisywać swoje myśli i umieszczać je w kontekście. Wreszcie, konkretne zadania zostały określone i mogliśmy przejść do fazy pracy w zorganizowany sposób."
   :testimonials.bjorn/author "Björn Ebbinghaus, Asystent ds. badań"

   :testimonials.lokay/company "Mediator i doradca ds. rozwiązywania konfliktów"
   :testimonials.lokay/quote "Miałem zaszczyt udzielić kolegom informacji zwrotnej w fazie początkowej i jestem pod wrażeniem tego ducha wartości i praktycznej orientacji.."
   :testimonials.lokay/author "Oliver Lokay, Mediator i doradca ds. rozwiązywania konfliktów"

   :testimonials.hck/company "Chief Digital Officer"
   :testimonials.hck/quote "Jako ekspert w dziedzinie cyfrowej transformacji w firmach, szybko dostrzegłem potencjał schnaq i od tego czasu jestem dostępny dla zespołu jako mentor. Silny pomysł i kompetentny zespół założycielski, o którym jeszcze usłyszymy!"
   :testimonials.hck/author "Hans-Christoph Kaiser, CDO"

   :testimonials.franky/company "FoxBase GmbH"
   :testimonials.franky/quote "Schnaq to nauka o rakietach na zapleczu, a trójkołowiec na froncie."
   :testimonials.franky/author "Frank Stampa, Head of Sales"

   :testimonials.metro/company "Metro Digital"
   :testimonials.metro/quote "Jako Asyncronous Working Evangelist, naprawdę doceniam schnaq za przełamywanie silosów informacyjnych i udostępnianie ich wszystkim pracownikom w przejrzysty i jasny sposób."
   :testimonials.metro/author "Dr. Tobias Schröder, Product Manager"

   :testimonials.eugenbialon/company "EugenBialonArchitekt GmbH"
   :testimonials.eugenbialon/quote "W biurze architektonicznym istnieje kilka równoległych projektów z dużą liczbą zaangażowanych podmiotów. Schnaq wspiera nas w międzyprojektowym zarządzaniu informacją, czy to w biurze, w biurze domowym, czy na placu budowy!"
   :testimonials.eugenbialon/author "Dipl.-Ing. Eugen Bialon, Partner Zarządzający i Architekt, EugenBialonArchitekt GmbH"

   :testimonials.bialon/quote "Dzięki schnaq, jestem w stanie przetworzyć masę informacji na temat digitalizacji uniwersytetu w sposób uporządkowany i przejrzysty. Pozwala mi to na szybkie działanie w każdym kontekście projektowym."
   :testimonials.bialon/author "Raphael Bialon, Osobisty asystent prorektora ds. digitalizacji, Heinrich-Heine-Universität Düsseldorf"

   :testimonials.sensor/company "Przedsiębiorstwo z branży czujników i techniki pomiarowej"
   :testimonials.sensor/quote "Jako część procesu zapoznawania się z nowymi produktami usługowymi, użyliśmy schnaq do zebrania naszych pomysłów i otwartych pytań centralnie w całym zespole. Dzięki temu mogliśmy dobrze przygotować się do spotkań i odnieść się do konkretnych kwestii.\nTeraz zapisujemy pytania, omawiamy je i w ciągu trzech tygodni nadal możemy zrozumieć, co postanowiliśmy."
   :testimonials.sensor/author "Florian Clever, Konsultant klienta Automatyzacja procesów obsługi"

   :testimonials.bib/company "Asystent ds. badań"
   :testimonials.bib/quote "Byliśmy również w stanie stymulować dyskusję i wymianę pomiędzy studentami podczas wydarzeń online poprzez schnaq, co miało znaczący wpływ na sukces tych wydarzeń."
   :testimonials.bib/author "Frauke Kling, Asystent ds. badań"

   ;; User related
   :user.button/set-name "Zapisz nazwę"
   :user.button/set-name-placeholder "Twoje imię i nazwisko"
   :user.button/change-name "Zmień nazwę"
   :user.button/success-body "Nazwa zapisana pomyślnie"
   :user.set-name.modal/header "Wprowadź nazwę"
   :user.set-name.modal/primer "Nazwa jest wyświetlana dla innych uczestników schnaq."
   :user/login "Zaloguj się"
   :user/logout "Wyloguj się"
   :user/register "Zaloguj się / Zarejestruj się"
   :user.profile/settings "Ustawienia"
   :user.action/link-copied "Link skopiowany!"
   :user.action/link-copied-body "Udostępnij link innym, aby dać im dostęp."
   :user/edit-account "Zarządzaj kontem użytkownika"
   :user/edit-notifications "Zarządzaj powiadomieniami"
   :user/edit-hubs "Zarządzaj węzłami"
   :user.settings "Ustawienia"
   :user.keycloak-settings "Ustawienia zaawansowane"
   :user.settings/header "Zarządzaj danymi użytkownika"
   :user.settings/info "Dane osobowe"
   :user.settings/notifications "Powiadomienia"
   :user.settings/hubs "Piasty"
   :user.settings/change-name "Zmień nazwę"
   :user.settings.button/change-account-information "Zapisz zmiany"
   :user.settings.profile-picture-title/success "Zdjęcie profilowe ustawione pomyślnie"
   :user.settings.profile-picture-body/success "Zdjęcie profilowe zostało załadowane i zapisane. Jeśli to konieczne, przeładuj stronę, aby zobaczyć zaktualizowany obraz."
   :user.settings.profile-picture-title/error "Przesyłanie zdjęcia profilowego nie powiodło się"
   :user.settings.profile-picture-too-large/error "Twoje zdjęcie profilowe ma rozmiar %d bajtów, maksymalny dozwolony rozmiar to %d bajtów. Proszę załadować mniejsze zdjęcie."
   :user.settings.profile-picture.errors/scaling "Twoje zdjęcie profilowe nie mogło zostać przekonwertowane. Może obraz jest uszkodzony. Proszę spróbować innego zdjęcia lub skontaktować się z nami."
   :user.settings.profile-picture.errors/invalid-file-type "Twoje zdjęcie profilowe ma nieprawidłowy typ pliku. Dozwolone są: %s"
   :user.settings.profile-picture.errors/default "Coś poszło nie tak podczas przesyłania obrazu. Proszę spróbować ponownie."

   ;; notification settings
   :user.notifications/header "Zarządzaj powiadomieniami"
   :user.notifications/mails "Powiadomienia e-mail"
   :user.notifications/info "Będziesz otrzymywać powiadomienia tylko wtedy, gdy pojawią się nowe posty w odwiedzonych przez Ciebie schnaqs.."
   :user.notifications.set-all-to-read/button "Oznacz wszystkie jako przeczytane"
   :user.notifications.set-all-to-read/info "Nadal otrzymujesz powiadomienia ze starych dyskusji? Nie ma problemu, wystarczy ustawić wszystko jako przeczytane i otrzymywać powiadomienia tylko o nowych dyskusjach.."

   ; mail interval
   :notification-mail-interval/daily "Codziennie"
   :notification-mail-interval/weekly "Tygodnik"
   :notification-mail-interval/never "Nigdy"

   ;; Errors
   :errors/generic "Wystąpił błąd"

   :error.generic/contact-us [:span "Jeśli znalazłeś się tutaj po kliknięciu na coś na schnaq.com, proszę daj nam znać na stronie " [:a {:href "mailto:info@schnaq.com"} "info@schnaq.com"]]

   :error.404/heading "Ta strona nie istnieje 🙉"
   :error.404/body "Niestety, adres URL, który podałeś nie istnieje. Mogła pojawić się literówka lub o jeden znak za dużo."

   :error.403/heading "Nie masz uprawnień, aby oglądać tę stronę 🧙‍♂️"
   :error.403/body "Nie masz uprawnień do dostępu do tej strony lub w adresie URL jest literówka."

   :error.beta/heading "Nie masz uprawnień, aby oglądać tę stronę 🧙‍♂️"
   :error.beta/body "Ta funkcja jest dostępna tylko dla beta testerów. Jeśli jesteś jednym z testerów, prosimy o wpisanie się na listę. Jeśli chciałbyś zostać beta testerem, napisz do nas na adres hello@schnaq.com."

   ;; Graph Texts
   :graph.button/text "Mindmap"
   :graph.download/as-png "Pobierz mapę myśli jako obraz"
   :graph.settings/title "Ustawienia dla mapy myśli"
   :graph.settings/description "Znajdź ustawienia dla swojej mapy myśli tutaj! Pobaw się suwakami i zobacz co się stanie."
   :graph.settings.gravity/label "Tutaj ustawiamy grawitację pomiędzy węzłami."
   :graph.settings/stabilize "Ustabilizuj mapę myśli"

   ;; Pricing Page
   :pricing.intro/heading "Wkrótce się zacznie!"
   :pricing.intro/lead "Już wkrótce będziesz mógł zarezerwować swoją taryfę tutaj. Czy chcieliby Państwo wziąć udział w naszej wersji beta i przetestować taryfę biznesową na wyłączność i bezpłatnie? Proszę się z nami skontaktować!"
   :pricing.free-tier/title "Starter"
   :pricing.free-tier/subtitle "Indywidualnie"
   :pricing.free-tier/description "Dla małych zespołów i do użytku prywatnego. Plan startowy jest doskonałym wprowadzeniem do dyskusji strukturalnych!"
   :pricing.free-tier/beta-notice "Po zakończeniu fazy beta, plan jest nadal dostępny dla maksymalnie pięciu użytkowników na zespół."
   :pricing.free-tier/call-to-action "Rozpocznij bezpłatnie"
   :pricing.free-tier/for-free "Stale bezpłatnie"
   :pricing.business-tier/title "Business"
   :pricing.business-tier/subtitle "Zbierz swój zespół"
   :pricing.business-tier/description "Pozwól, aby nasz A.I. wspierał Cię i dowiedział się więcej o Twoich dyskusjach!"
   :pricing.business-tier/call-to-action "Przetestuj biznes teraz"
   :pricing.enterprise-tier/title "Enterprise"
   :pricing.enterprise-tier/subtitle "Wielkie plany?"
   :pricing.enterprise-tier/description "Chcesz połączyć całą firmę, klub, instytucję lub nawet całą uczelnię? W takim razie trafiłeś we właściwe miejsce!"
   :pricing.enterprise-tier/call-to-action "Wyślij zapytanie"
   :pricing.enterprise-tier/on-request "Na żądanie"
   :pricing.features/implemented "Już wdrożone"
   :pricing.features/to-be-implemented "Wkrótce dostępne"
   :pricing.features/starter ["Hosting w Niemczech" "Tworzenie dyskusji" "Automatyczna Mindmap" "Udostępnianie przez link" "Eksport tekstu i obrazu"]
   :pricing.features/business ["Tablica rozdzielcza analizy" "Podsumowania A.I." "Przestrzeń osobista"]
   :pricing.features/enterprise ["Osadzanie w istniejących systemach\" \"Logowanie SSO (OpenID, LDAP, ...)" "Whitelabelling" "On-Premise"]
   :pricing.features/upcoming ["Analiza nastrojów A.I." "Przekształcanie mowy na tekst"]
   :pricing.units/per-month "/ Miesiąc"
   :pricing.units/per-active-account "dla rachunku aktywów"
   :pricing.notes/with-vat "plus VAT."
   :pricing.trial/call-to-action "Test Business przez 30 dni"
   :pricing.trial/description "Karta kredytowa nie jest konieczna! Możliwość odwołania w dowolnym momencie."
   :pricing.trial.temporary/deactivation "Dostępne od 01.11.2021 r."
   :pricing.features/heading "Zalety subskrypcji Schnaq"
   :pricing.features.user-numbers/heading "Nieograniczona liczba uczestników"
   :pricing.features.user-numbers/content "Pozwól współpracować tylu pracownikom, ilu chcesz. *"
   :pricing.features.team-numbers/heading "Nieograniczone zespoły"
   :pricing.features.team-numbers/content "Liczba zespołów, które możesz utworzyć jest nieograniczona. *"
   :pricing.features.app-integration/heading "Integracja aplikacji"
   :pricing.features.app-integration/content "Połącz schnaq łatwo z Twoim Slack, MS Teams, Confluence …"
   :pricing.features.analysis/heading "Analizy automatyczne"
   :pricing.features.analysis/content "Wkłady są automatycznie analizowane i przygotowywane dla wszystkich uczestników.."
   :pricing.features.knowledge-db/heading "Baza danych wiedzy"
   :pricing.features.knowledge-db/content "Gromadzenie zdobytej wiedzy i pomysłów w jednym miejscu."
   :pricing.features.mindmap/heading "Interaktywna mapa myśli"
   :pricing.features.mindmap/content "Wszystkie wkłady są automatycznie wyświetlane graficznie i interaktywnie."
   :pricing.features/disclaimer "* Obowiązuje tylko dla subskrypcji Business"
   :pricing.competitors/per-month-per-user " € miesięcznie za użytkownika"
   :pricing.faq/heading "Najczęściej zadawane pytania dotyczące subskrypcji schnaq"
   :pricing.faq.terminate/heading "Czy mogę zrezygnować w dowolnym momencie?"
   :pricing.faq.terminate/body
   [:<> [:span.text-primary "Tak!"] " Możesz" [:span.text-primary " co miesiąc"] " anulować,
     jeśli wybrałeś metodę płatności miesięcznej. W przypadku wyboru metody płatności rocznej
     możesz zrezygnować na koniec roku subskrypcji."]
   :pricing.faq.extra-price/heading "Czy muszę płacić dodatkowo za więcej osób?"
   :pricing.faq.extra-price/body
   [:<> [:span.text-primary "Nie, "] "możesz" [:span.text-primary " dowolna liczba osób "]
    " do swojej organizacji. Każda firma, stowarzyszenie,
    instytucja edukacyjna, itp. potrzebuje tylko " [:span.text-primary "jeden abonament."]]
   :pricing.faq.trial-time/heading "Czy okres próbny przedłuża się automatycznie?"
   :pricing.faq.trial-time/body
   [:<> [:span.text-primary "Nie, "] "po zakończeniu okresu próbnego, można" [:span.text-primary " aktywnie zdecydować"]
    " czy chcesz dodać dane dotyczące płatności i nadal korzystać z taryfy biznesowej.
    The " [:span.text-primary "Starter Plan pozostaje bezpłatny na czas nieokreślony"] ", nawet po okresie próbnym."]
   :pricing.faq.longer-trial/heading "Czy mogę dłużej testować taryfę Biznes?"
   :pricing.faq.longer-trial/body
   [:<> [:span.text-primary "Tak! "] "Po prostu napisz do nas " [:span.text-primary "E-mail"] " do "
    [:a {:href "mailto:info@schnaq.com"} "info@schnaq.com."]]
   :pricing.faq.privacy/heading "Kto ma dostęp do moich danych?"
   :pricing.faq.privacy/body-1
   [:<> "Każda osoba, którą dodasz do swojej firmy, może potencjalnie uzyskać dostęp do przechowywanych danych.
    Z technicznego punktu widzenia, dane są przechowywane całkowicie bezpiecznie na"
    [:span.text-primary " Niemieckie serwery i zgodność z GDPR"] " zapisane. Na naszej "]
   :pricing.faq.privacy/body-2 "Strona poświęcona bezpieczeństwu danych"
   :pricing.faq.privacy/body-3 " znajdziesz więcej informacji"
   :pricing/headline "Abonamenty"
   :pricing.newsletter/lead "Bądź informowany natychmiast, gdy subskrypcja wejdzie w życie:"
   :pricing.newsletter/name "newsletter schnaq."

   :schnaq.startpage.cta/button "Z kolegium schnaqqen"

   ;; Tooltips
   :tooltip/history-statement "Wracając do wkładu"
   :tooltip/history-statement-current "Bieżąca składka"

   ;; History
   :history/title "Kurs"
   :history.home/text "Start"
   :history.home/tooltip "Powrót do początku dyskusji"
   :history.statement/user "Wkład z"
   :history.all-schnaqs/tooltip "Powrót do przeglądu schnaqs"
   :history.back/tooltip "Powrót do poprzedniego postu"

   ;; Route Link Texts
   :router/admin-center "Centrum Administracyjne"
   :router/all-feedbacks "Wszystkie opinie"
   :router/analytics "Pulpit analityczny"
   :router/create-schnaq "Utwórz schnaq"
   :router/graph-view "Widok wykresu"
   :router/how-to "Jak używać schnaq?"
   :router/last-added-schnaq "Ostatnio utworzony schnaq"
   :router/visited-schnaqs "Odwiedził schnaqs"
   :router/not-found-label "Nie znaleziono Przekierowanie trasy"
   :router/pricing "Wycena"
   :router/privacy "Ochrona danych"
   :router/qanda "PYTANIA I ODPOWIEDZI"
   :router/start-discussion "Rozpocznij dyskusję"
   :router/startpage "Strona główna"
   :router/true-404-view "Strona z błędem 404"
   :router/code-of-conduct "Zasady postępowania"
   :router/summaries "Streszczenia"
   :router/alphazulu "ALPHAZULU"

   :admin.center.start/title "Centrum Administracyjne"
   :admin.center.start/heading "Centrum Administracyjne"
   :admin.center.start/subheading "Administracja schnaqs jako superużytkownik"
   :admin.center.delete/confirmation "Czy ten schnaq naprawdę powinien zostać usunięty?"
   :admin.center.delete.public/button "Usuń schnaq"
   :admin.center.delete/heading "Usuń"
   :admin.center.delete.private/label "Share-hash"
   :admin.center.delete.private/heading "Prywatne schnaqs"

   :badges.sort/newest "Najnowsze"
   :badges.sort/popular "Popularny"
   :badges.sort/alphabetical "Alfabetycznie"
   :badges.filters/button "Filtr"

   :filters.label/filter-for "Filtrowanie według"
   :filters.add/button "Dodaj filtr"
   :filters.option.labels/text "Etykieta"
   :filters.option.labels/includes "zawiera"
   :filters.option.labels/excludes "nie obejmuje"
   :filters.option.type/text "Rodzaj składki"
   :filters.option.type/is "jest"
   :filters.option.type/is-not "nie jest"
   :filters.option.votes/text "Głosy"
   :filters.option.vote/bigger "więcej niż"
   :filters.option.vote/equal "to samo"
   :filters.option.vote/less "mniej niż"
   :filters.buttons/clear "Wyczyść wszystkie filtry"
   :filters.heading/active "Filtry aktywne"

   ;; Labels for programmatically created text in label overview
   :filters.labels.type/labels "Etykiety"
   :filters.labels.type/type "Rodzaj składki"
   :filters.labels.type/votes "Głosy"
   :filters.labels.criteria/includes "zamieścić"
   :filters.labels.criteria/excludes "nie zawierać"
   :filters.labels.criteria/is "jest"
   :filters.labels.criteria/is-not "nie jest"
   :filters.labels.criteria/> "są większe niż"
   :filters.labels.criteria/= "to samo"
   :filters.labels.criteria/< "są mniejsze niż"
   :filters.stype/neutral "neutralny"
   :filters.stype/attack "z drugiej strony"
   :filters.stype/support "w tym celu"

   :filters.discussion.option.state/label "Status schnaq"
   :filters.discussion.option.state/closed "zamknięta"
   :filters.discussion.option.state/read-only "dostęp tylko do odczytu"
   :filters.discussion.option.numbers/label "Liczba składek"
   :filters.discussion.option.author/label "Udział własny"
   :filters.discussion.option.author/prelude "I"
   :filters.discussion.option.author/included "brać udział"
   :filters.discussion.option.author/excluded "nie uczestniczyć"
   ;; Auto-generation of pretty-labels
   :filters.labels.criteria/included "udział"
   :filters.labels.criteria/excluded "nie uczestniczyć"
   :filters.labels.type/state "Status schnaq"
   :filters.labels.type/numbers "Liczba składek"
   :filters.labels.type/author "Ty"

   :loading.placeholder/lead "Dane są ładowane..."
   :loading.placeholder/takes-too-long "Trwa to dłużej niż oczekiwano. Może coś poszło nie tak. Spróbuj przeładować stronę lub powtórzyć proces ponownie. Jeśli nadal masz problemy, skontaktuj się z nami!"

   :hubs/heading "Hubs"
   :hub/heading "Osobiste %s Hub"
   :hub/settings "Administracja"
   :hub.settings/change-name "Zmień nazwę koncentratora"
   :hub.settings.name/updated-title "Zmiana nazwy piasty"
   :hub.settings.name/updated-body "Nazwa koncentratora została pomyślnie zmieniona!"
   :hub.settings.update-logo-title/success "Logo Piasta zostało pomyślnie zmienione!"
   :hub.settings.update-logo-body/success "Twoje nowe logo zostało pomyślnie załadowane. Jeśli to konieczne, przeładuj stronę, aby wyświetlić zaktualizowany obraz."
   :hub.settings/save "Zapisz ustawienia"
   :hub.add.schnaq.success/title "Schnaq dodany!"
   :hub.add.schnaq.success/body "Schnaq został pomyślnie dodany do Twojego koncentratora."
   :hub.add.schnaq.error/title "Błąd podczas dodawania!"
   :hub.add.schnaq.error/body "Nie udało się znaleźć lub dodać schnaq. Proszę spróbować ponownie."
   :hub.add.schnaq.input/label "Dodaj schnaq"
   :hub.add.schnaq.input/placeholder "URL Schnaq np. https://schnaq.com/schnaq/... lub częściowy kod"
   :hub.add.schnaq.input/button "Dodaj shnaq"
   :hub.remove.schnaq.success/title "schnaq usunięty!"
   :hub.remove.schnaq.success/body "Schnaq został pomyślnie usunięty z twojego koncentratora."
   :hub.remove.schnaq.error/title "Usunięcie nie powiodło się!"
   :hub.remove.schnaq.error/body "Coś poszło nie tak podczas demontażu. Proszę spróbować ponownie."
   :hub.remove.schnaq/prompt "Czy schnaq naprawdę powinien być usunięty z piasty?"
   :hub.remove.schnaq/tooltip "Usuń program Schnaq z koncentratora."
   :hub.members/heading "Członkowie"

   :hub.members.add.result.success/title "Sukces"
   :hub.members.add.result.success/body "Użytkownik został pomyślnie dodany do koncentratora"
   :hub.members.add.result.error/title "Błąd"
   :hub.members.add.result.error/unregistered-user "Nie ma konta schnaq pod adresem e-mail, którego szukasz."
   :hub.members.add.result.error/generic-error "Coś poszło nie tak. Sprawdź wiadomość e-mail i spróbuj ponownie."
   :hub.members.add.form/title "Dodaj członków"
   :hub.members.add.form/button "Dodaj użytkownika!"

   :schnaq.search/heading "Wyniki wyszukiwania"
   :schnaq.search/results "Wyniki"
   :schnaq.search/no-input "Brak danych wejściowych"
   :schnaq.search/title "Szukaj"
   :schnaq.search/input "Szukaj…"
   :schnaq.search/new-search-title "Brak wyników"

   :lead-magnet.privacy/consent "Chciałbym otrzymać listę kontrolną dla pracy zgodnej z ochroną danych jako plik .pdf przez e-mail i niniejszym zapisuję się do newslettera schnaq, aby regularnie otrzymywać informacje od schnaq.com w przyszłości."
   :lead-magnet.form/button "Wyślij mi listę kontrolną!"
   :lead-magnet/heading "Praca rozproszona zgodna z ochroną danych"
   :lead-magnet/subheading "Podręczna lista kontrolna, aby być przygotowanym we wszystkich obszarach"
   :lead-magnet.cover/alt-text "Okładka listy kontrolnej dotyczącej praw do ochrony danych osobowych w dystrybucji pracy"
   :lead-magnet.form/label "Link do pobrania pliku PDF prześlemy pocztą elektroniczną"
   :lead-magnet.requested/part-1 "Twój pierwszy krok w kierunku pracy zgodnej z ochroną danych został wykonany!"
   :lead-magnet.requested/part-2 "W ciągu kilku minut powinieneś otrzymać link do pobrania. Sprawdź również folder spamu."
   :lead-magnet.cta/button "Bezpośrednio do listy kontrolnej"
   :lead-magnet.explain.what/heading "Jak wygląda lista kontrolna dotycząca pracy zdalnej zgodnej z zasadami ochrony danych?"
   :lead-magnet.explain.what/text "Sprawdziliśmy aktualne oprogramowanie do pracy zdalnej pod kątem zgodności z ochroną danych (w szczególności GDPR).
   Wyniki zostały podsumowane w formie listy kontrolnej. Zawiera wszystkie ważne kategorie, które są potrzebne do elastycznej pracy.
   W ten sposób można zwracać uwagę na ochronę danych, niezależnie od tego, czy pracuje się w domu, w podróży czy w hybrydowym miejscu pracy."
   :lead-magnet.explain.how/heading "Jak działa lista kontrolna?"
   :lead-magnet.explain.how/text "Lista kontrolna jest uporządkowana według kategorii. Dla każdej kategorii, takich jak platformy czatu, istnieje kilka alternatyw.
   Każda alternatywa jest oznaczona kolorem sygnalizacji świetlnej. Kolor zielony oznacza nieszkodliwość w momencie przeprowadzania naszego testu. Podczas gdy kolor czerwony ostrzega przed
   ostrzega o możliwych problemach. Wreszcie, pomarańczowy często ma drobne problemy, ale w zasadzie jest OK.
   Oprócz kolorów sygnalizacji świetlnej, znajdują się tam również objaśniające punkty kluczowe do dalszych badań."

   :summary.link.button/text "Analiza"
   :summary.user.request-succeeded/label "Wymagane podsumowanie. Proszę chwilę poczekać."
   :summary.user/computation-time "Utworzenie podsumowania może potrwać kilka minut."
   :summary.user.requested/label "Wymagane jest podsumowanie"
   :summary.user.not-requested/label "Streszczenie wniosku"
   :summary.user.abort/confirm "Obliczenia mogą trwać kilka minut. Czy naprawdę chcesz zrezygnować?"
   :summary.user.abort/label "Problemy z obliczeniami?"
   :summary.user.abort/button "Anuluj"
   :summary.user/privacy-warning "Dla celów ulepszenia, członkowie zespołu schnaq będą mogli poufnie przeglądać zawartość streszczenia.."
   :summary.user/label "Streszczenie:"
   :summary.user/last-updated "Ostatnia aktualizacja:"
   :summary.user/heading "Streszczenia"
   :summary.user/subheading "Spójrz na dyskusję w kilku zdaniach."
   :summary.admin/open-summaries "Otwarte streszczenia: %s"
   :summary.admin/closed-summaries "Zamknięte streszczenia: %s"
   :summary.admin/discussion "Dyskusja"
   :summary.admin/requester "Wnioskowane przez"
   :summary.admin/requested-at "Wnioskowano w dniu"
   :summary.admin/summary "Streszczenie"
   :summary.admin/submit "Wyślij"
   :summary.admin/closed-at "Zamknięte w dniu"})
