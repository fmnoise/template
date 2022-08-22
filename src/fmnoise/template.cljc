(ns fmnoise.template
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.set :as set]
            #?@(:cljs [[goog.string :as gstring]
                       [goog.string.format]])))

(defn template
  "Replaces placeholders in template form (string or data structure) with values from supplied map.
  Keys in map can be either keywords or symbols or strings.
  String placeholder shoud be valid keyword name (without colon) in curly/square/angle braces (configurable) eg {name}, {user/name}, [user/name], <user/name>.
  String placeholder can contain desired format separated with colon eg {order/date:%tD}.
  Data placeholder should be valid keyword name (without colon) in angle braces represented as keyword eg :<name> or :<user/name>.
  Data placeholders can't contain formatting.
  First argument could be either template string, template data structure (if `:=>` key is missing) or options map.
  If first argument is a template data structure, options could be supplied as structure metadata.
  The following options are supported:
  `:=>` - template form: string or data structure
  `:default` - sets a default for all missing values
  `:defaults` - a map with default values for certain keys
  `:resolve?` or `:resolve` (when supplied as meta) - determines if var lookup/resolve should be performed (defaults to false)
  `:throw?` or `:throw` (when supplied as meta) - specifies if exception should be thrown in case of missing value and not having any defaults (defaults to false)
  `:throw-on` - a set with keys which should be always provided either through values map or defaults map
  `:brackets` - a keyword indicating types of brackets used for placeholders in template string. Valid options are:
     - `:curly` (default) eg {user/name}
     - `:square` eg [user/name]
     - `:angle` eg <user/name>
  "
  ([options] (partial template options))
  ([{:keys [=> default defaults resolve? throw? throw-on brackets] :as options} values]
   (if (or (string? options) (and (some? options) (nil? =>)))
     (-> options meta (assoc :=> options) (set/rename-keys {:throw :throw? :resolve :resolve?}) (template values))
     (when =>
       (let [data? (not (string? =>))
             placeholder (if data?
                           #":\<([^\>]+)\>"
                           (case brackets :square #"\[([^\]]+)\]" :angle #"\<([^\>]+)\>" #"\{([^\}]+)\}"))
             replace-fn (fn [[_ m]]
                          (let [[var fmt] (if data? [m] (str/split m #":"))
                                kvar (keyword var)
                                svar (symbol var)
                                value (or (get values kvar) (get defaults kvar)
                                          (get values var) (get defaults var)
                                          (get values svar) (get defaults svar)
                                          (when resolve? (some-> svar resolve deref))
                                          (when (and throw-on
                                                     (or (contains? throw-on kvar)
                                                         (contains? throw-on var)
                                                         (contains? throw-on svar)))
                                            (throw (ex-info (str "Missing value for template variable: " var) (assoc options :values values))))
                                          default
                                          (when throw?
                                            (throw (ex-info (str "Missing value for template variable: " var) (assoc options :values values)))))
                                output (if data? pr-str str)
                                value (if (fn? value) (value) value)]
                            (if (and fmt value)
                              (try
                                #?(:clj (format fmt value) :cljs (gstring/format fmt value))
                                (catch #?(:clj Exception :cljs :default) e
                                  (throw (ex-info (str "Failed to format template variable " var  " as " fmt " with " value)
                                                  (assoc options :values values)
                                                  e))))
                              (output value))))]
         (cond-> (str/replace (str =>) placeholder replace-fn)
           data? edn/read-string))))))
