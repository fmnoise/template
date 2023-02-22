(ns fmnoise.template
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.set :as set]
            #?@(:cljs [[goog.string :as gstring]
                       [goog.string.format]])))

(defn template-keys
  "Returns set of keys used in given `template` form (string or data). Can accept options as optional 2nd argument:
  `:as` - determines how to interpret the keys, possible values are: `:symbols`, `:strings`, `:keywords` (default)
  `:brackets` - a keyword indicating types of brackets used for placeholders in string template form. Valid options are:
   - `:curly` (default) eg {user/name}
   - `:square` eg [user/name]
   - `:angle` eg <user/name>"
  {:added "0.2.0"}
  ([form] (template-keys form nil))
  ([form {:keys [brackets as]}]
   (let [data? (not (string? form))
         output (case as
                  :symbols symbol
                  :strings identity
                  keyword)
         placeholder (case brackets
                       :square #"\[([^\]]+)\]"
                       :angle #"\<([^\>]+)\>"
                       (if data? #":\<([^\>]+)\>" #"\{([^\}]+)\}"))]
     (into #{}
           (map (comp output first #(str/split % #":") last))
           (re-seq placeholder (if data? (str form) form))))))

(defn template
  "Replaces placeholders in template form (string or data structure) with values from supplied source (should compatible with `clojure.core/get`).
  Keys in map can be either keywords or symbols or strings.
  String placeholder shoud be valid keyword name (without colon) in curly/square/angle braces (configurable) eg {name}, {user/name}, [user/name], <user/name>.
  String placeholder can contain desired format separated with colon eg {order/date:%tD}.
  Data placeholders is by default keywords in angle braces eg :<user/name>, but could be configured as square or angle braced values (without being keyword) eg '[user/name] or '<user/name>.
  Data placeholders can't contain formatting.
  First argument could be either template string, template data structure (if `:=>` key is missing) or options map.
  If first argument is a template data structure, options could be supplied as structure metadata.
  The following options are supported:
  `:=>` - template form: string or data structure
  `:default` - sets a default for all missing values
  `:defaults` - a map with default values for certain keys
  `:on-missing` - a 1-arg function which accepts a map with `:name` (contains placeholder name) and `:values` (contains values map)
  `:throw?` or `:throw` (when supplied as meta) - specifies if exception should be thrown in case of missing value and not having any defaults (defaults to false)
  `:remove-nils?` or `:remove-nils` (when supplied as meta) - specifies if binding for missing value should be removed. Makes the most sense for data templates.
  `:throw-on` - a set with keys which should be always provided either through values map or defaults map
  `:brackets` - a keyword indicating types of brackets used for placeholders in template string. Valid options are:
     - `:curly` (default) eg {user/name}
     - `:square` eg [user/name]
     - `:angle` eg <user/name>

  Value for each key is resolved in the following order:
  1. `keyword` key lookup in supplied values and defaults (if present)
  2. `string` key lookup in supplied values and defaults (if present)
  3. `symbol` key lookup in supplied values and defaults (if present)
  4. if `on-missing` is provided, it's called with key name and values object
  5. if `throw-on` is provided and has either keyword or string or symbol key then `ExceptionInfo` is thrown
  6. if `default` is provided, it's used
  7. if `throw?` is set to true then `ExceptionInfo` is thrown
  "
  ([options] (partial template options))
  ([{:keys [=> default defaults on-missing throw? remove-nils? throw-on brackets] :as options} values]
   (if (or (string? options) (and (some? options) (nil? =>)))
     (-> options meta (assoc :=> options) (set/rename-keys {:throw :throw? :remove-nils :remove-nils?}) (template values))
     (when =>
       (let [data? (not (string? =>))
             placeholder (case brackets
                           :square #"\[([^\]]+)\]"
                           :angle #"\<([^\>]+)\>"
                           (if data? #":\<([^\>]+)\>" #"\{([^\}]+)\}"))
             replace-fn (fn [[_ m]]
                          (let [[var fmt] (if data? [m] (str/split m #":"))
                                kvar (keyword var)
                                svar (symbol var)
                                value (or (get values kvar) (get defaults kvar)
                                          (get values var) (get defaults var)
                                          (get values svar) (get defaults svar)
                                          (when on-missing
                                            (on-missing {:name var :values values :options options}))
                                          (when (and throw-on
                                                     (or (contains? throw-on kvar)
                                                         (contains? throw-on var)
                                                         (contains? throw-on svar)))
                                            (throw (ex-info (str "Missing value for template variable: " var)
                                                            {:options options :values values})))
                                          default
                                          (when throw?
                                            (throw (ex-info (str "Missing value for template variable: " var)
                                                            {:options options :values values}))))
                                output (if data? pr-str str)
                                value (if (fn? value) (value) value)]
                            (if (and fmt value)
                              (try
                                #?(:clj (format fmt value) :cljs (gstring/format fmt value))
                                (catch #?(:clj Exception :cljs :default) e
                                  (throw (ex-info (str "Failed to format template variable " var  " as " fmt " with " value)
                                                  {:options options :values values}
                                                  e))))
                              (if (and (nil? value) remove-nils?) "" (output value)))))]
         (cond-> (str/replace (str =>) placeholder replace-fn)
           data? edn/read-string))))))
