(ns schnaq.interface.components.animal-avatars
  (:require ["react-animals$default" :as Animal]
            [clojure.string :as str]
            [goog.string :as gstring]
            [re-frame.core :as rf]))

(def ^:private animals #{"alligator",
                         "anteater",
                         "armadillo",
                         "auroch",
                         "axolotl",
                         "badger",
                         "bat",
                         "beaver",
                         "buffalo",
                         "camel",
                         "capybara",
                         "chameleon",
                         "cheetah",
                         "chinchilla",
                         "chipmunk",
                         "chupacabra",
                         "cormorant",
                         "coyote",
                         "crow",
                         "dingo",
                         "dinosaur",
                         "dolphin",
                         "duck",
                         "elephant",
                         "ferret",
                         "fox",
                         "frog",
                         "giraffe",
                         "gopher",
                         "grizzly",
                         "hedgehog",
                         "hippo",
                         "hyena",
                         "ibex",
                         "ifrit",
                         "iguana",
                         "jackal",
                         "kangaroo",
                         "koala",
                         "kraken",
                         "lemur",
                         "leopard",
                         "liger",
                         "llama",
                         "manatee",
                         "mink",
                         "monkey",
                         "moose",
                         "narwhal",
                         "orangutan",
                         "otter",
                         "panda",
                         "penguin",
                         "platypus",
                         "pumpkin",
                         "python",
                         "quagga",
                         "rabbit",
                         "raccoon",
                         "rhino",
                         "sheep",
                         "shrew",
                         "skunk",
                         "squirrel",
                         "tiger",
                         "turtle",
                         "walrus",
                         "wolf",
                         "wolverine",
                         "wombat"})

(def ^:private colors {"amaranth" "#9F2B68",
                       "amber" "#FFBF00",
                       "amethyst" "#9966CC",
                       "apricot" "#FDD5B1",
                       "aqua" "#BED3E5",
                       "aquamarine" "#7FFFD4",
                       "azure" "#0080FF",
                       "beige" "#FFF8E7",
                       "black" "#000000",
                       "blue" "#1292EE",
                       "blush" "#DE5D83",
                       "bronze" "#CD7F32",
                       "brown" "#964B00",
                       "chocolate" "#7B3F00",
                       "coffee" "#6F4E37",
                       "copper" "#B87333",
                       "coral" "#FF7F50",
                       "crimson" "#DC143C",
                       "cyan" "#00FFFF",
                       "emerald" "#50C878",
                       "fuchsia" "#FF00FF",
                       "gold" "#FFD700",
                       "gray" "#808080",
                       "green" "#7CFC00",
                       "harlequin" "#3fff00",
                       "indigo" "#8A2BE2",
                       "ivory" "#FFFFF0",
                       "jade" "#00A36C",
                       "lavender" "#E6E6FA",
                       "lime" "#32CD32",
                       "magenta" "#8B008B", ;; we use darkmagenta to not use the trademarked color
                       "maroon" "#800000",
                       "moccasin" "#FFE4B5",
                       "olive" "#808000",
                       "orange" "#FFA500",
                       "peach" "#FFCBA4",
                       "pink" "#FFC0CB",
                       "plum" "#673147",
                       "purple" "#A020F0",
                       "red" "#FF0000",
                       "rose" "#F33A6A",
                       "salmon" "#FA8072",
                       "sapphire" "#0F52BA",
                       "scarlet" "#BB0000",
                       "silver" "#C0C0C0",
                       "tan" "#D2B48C",
                       "teal" "#008080",
                       "tomato" "#FF6347",
                       "turquoise" "#40E0D0",
                       "violet" "#EE82EE",
                       "white" "FFFFFF",
                       "yellow" "#FFFF00"})

(defn generate-animal-avatar
  "Generate an identicon. Returns xml-styled SVG."
  [& {:keys [name size]}]
  (let [name-parts (str/split name #" ")
        two-part? (= 2 (count name-parts))
        color-part (str/lower-case (first name-parts))
        animal-part (when two-part? (str/lower-case (second name-parts)))
        animal (if (contains? animals animal-part)
                 animal-part
                 "elephant")
        color (get colors color-part "#1292ee")]
    [:> Animal {:size (gstring/format "%spx" size)
                :name animal
                :color color}]))

(defn automatic-animal-avatar
  "Generate the avatar without passing a name, just a size. Gets the name from the db"
  [& {:keys [size]}]
  [generate-animal-avatar :name @(rf/subscribe [:user/display-name]) :size size])