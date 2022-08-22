<p align="center"><img width="753" alt="image" src="https://user-images.githubusercontent.com/4033391/186002944-0e379c40-71d0-4e4b-9d38-432a18f50f25.png"></p>

Pure Clojure(Script) zero-dependencies templating library

## Usage

```clj
(require '[fmnoise.template :refer [template]])
```

### String templating

```clj
(def tstr "Hi {customer/name}! Your order{order/id: #%d} is scheduled{order/date: for %tD}.")

(template tstr {:customer/name "Alex" :order/id 12345 :order/date #inst "2022-08-29T12:34:00"})
;; => "Hi Alex! Your order #12345 is scheduled for 08/29/22."

(template tstr nil)
;; => "Hi ! Your order is scheduled."

(template {:=> tstr :defaults {:customer/name "Dear Customer"}} {:order/id 12345})
;; => "Hi Dear Customer! Your order #12345 is scheduled."

(template {:=> tstr :throw-on #{:order/date}} {:order/id 12345})
;; Missing value for template variable: order/date

(template {:=> tstr :throw? true} {:order/id 12345})
;; Missing value for template variable: customer/name

(template {:=> "Order #[order/id] is scheduled for [order/date:%tD]" :brackets :square}
          {:order/id 12345 :order/date #inst "2022-09-18"})
;; => "Order #12345 is scheduled for 09/18/22"

(template {:=> "Order #<order/id> is scheduled for <order/date:%tD>" :brackets :angle}
          {:order/id 12345 :order/date #inst "2022-09-18"})
;; => "Order #12345 is scheduled for 09/18/22"
```

### Data templating

```clj
(template [:db/add :<db/id> :user/email :<user/email>]
          {:db/id 12345 :user/email "user@company.com"})
;; => [:db/add 12345 :user/email "user@company.com"]

(template ^:throw [:db/add :<db/id> :user/email :<user/email>]
          {:user/email "user@company.com"})
;;  Missing value for template variable: db/id
```

### Values

Keys in values map for `template` could be either keywords or strings or symbols:
```clj
(template "Hi {user/name}!" {:user/name "Joe"})
;; => Hi Joe!

(template "Hi {user/name/same}!" {"user/name" "Jim"})
;; => Hi Jim!

(template "Hi {user/name}!" {'user/name "Jane"})
;; => Hi Jane!
```

Values could also be 0-arity functions:
```clj
(template "Winner is #{id}" {:id #(rand-int 1000)})
;; => "Winner is #939"

(template {:=> "Winner is #{id}" :defaults  {:id #(rand-int 1000)}} nil)
;; => "Winner is #456"

(template {:=> "Winner is #{id}" :default #(rand-int 1000)} nil)
;; => "Winner is #437"
```

### Auto-currying

When called with single argument, `template` returns function of values:

```clj
(def email-template
  (template {:=> "Hello {user/name}!" :defaults {:user/name "Dear User"}}))

(email-template {:user/name "John"})
;; => "Hello John!"

(email-template nil)
;; => "Hello Dear User!"

(def tx-template
  (template {:=> [[:db/add :<db/id> :user/rank :<user/rank>]]
             :defaults {:user/rank #(rand-int 1000)}
             :throw-on #{:db/id}}))

(tx-template {:db/id 12345})
;; => [[:db/add 12345 :user/rank 255]]

(tx-template {:user/rank 5000})
;; Missing value for template variable: db/id

```

## License

Copyright © 2022 fmnoise

Distributed under the [Eclipse Public License 2.0](http://www.eclipse.org/legal/epl-2.0)
