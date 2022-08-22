(ns fmnoise.template-test
  (:require [clojure.test :refer [deftest testing is]]
            [fmnoise.globals :as globals :refer [greeting]]
            [fmnoise.template :refer [template]]))

(deftest template-string-test
  (let [tstr "Welcome to {system}, {visitor/name}!"]
    (is (= "Welcome to Orion, Joe!" (template tstr {:visitor/name "Joe" :system "Orion"})))
    (is (= "Welcome to , !" (template tstr nil)))
    (testing "default"
      (is (= "Welcome to unknown, unknown!" (template {:=> tstr :default "unknown"} nil))))
    (testing "defaults"
      (is (= "Welcome to unknown, Guest!" (template {:=> tstr :default "unknown" :defaults {:visitor/name "Guest"}} nil))))
    (testing "format"
      (is (= "Today is 2022-12-08" (template "Today is {date:%tF}" {:date #inst "2022-12-08T12:30"}))))
    (testing "brackets"
      (is (= "I'm ok"
             (template "I'm {status}" {:status "ok"})
             (template {:=> "I'm <status>" :brackets :angle} {:status "ok"})
             (template {:=> "I'm [status]" :brackets :square} {:status "ok"})
             (template {:=> "I'm {status}" :brackets :curly} {:status "ok"})
             (template {:=> "I'm {status}" :brackets :unknown} {:status "ok"}))))
    (testing "value keys: kw, syms, strings"
      (is (= "I'm online"
             (template "I'm {user/status}" {:user/status "online"})
             (template "I'm {user/status}" {"user/status" "online"})
             (template "I'm {user/status}" {'user/status "online"})
             (template {:=> "I'm {user/status}" :defaults {:user/status "online"}} nil)
             (template {:=> "I'm {user/status}" :defaults {"user/status" "online"}} nil)
             (template {:=> "I'm {user/status}" :defaults {'user/status "online"}} nil))))
    (testing "value fn"
      (is (= "I'm online"
             (template "I'm {user/status}" {:user/status (constantly "online")})
             (template "I'm {user/status}" {"user/status" (constantly "online")})
             (template "I'm {user/status}" {'user/status (constantly "online")})
             (template {:=> "I'm {user/status}" :defaults {:user/status (constantly "online")}} nil)
             (template {:=> "I'm {user/status}" :defaults {"user/status" (constantly "online")}} nil)
             (template {:=> "I'm {user/status}" :defaults {'user/status (constantly "online")}} nil)
             (template {:=> "I'm {user/status}" :default (constantly "online")} nil))))
    (testing "throw?"
      (is (= "Welcome to _, _!" (template {:=> tstr :default "_" :throw? true} nil)))
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Missing value for template variable"
           (template {:=> tstr :throw? true} nil))))
    (testing "throw-on"
      (is (= "Welcome to matrix, ***!"
             (template {:=> tstr :throw-on #{:system} :default "***" :defaults {:system "matrix"}} nil)))
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Missing value for template variable"
           (template {:=> tstr :throw-on #{:system} :default "***"} nil))))))

(deftest template-data-test
  (let [ds [[:db/add :<db/id> :user/name :<user/name>]]]
    (is (= [[:db/add 123 :user/name "Joe"]] (template ds {:db/id 123 :user/name "Joe"})))
    (is (= [[:db/add nil :user/name nil]] (template ds nil)))
    (testing "default"
      (is (= [[:db/add "unknown" :user/name "unknown"]]
             (template {:=> ds :default "unknown"} nil))))
    (testing "defaults"
      (is (= [[:db/add "unknown" :user/name "Guest"]]
             (template {:=> ds :default "unknown" :defaults {:user/name "Guest"}} nil))))
    (testing "value keys: kw, syms, strings"
      (is (= [[:db/add 123 :user/name "Joe"]]
             (template ds {:db/id 123 :user/name "Joe"})
             (template ds {"db/id" 123 "user/name" "Joe"})
             (template ds {'db/id 123 'user/name "Joe"})
             (template {:=> ds :defaults {:db/id 123 :user/name "Joe"}} nil)
             (template {:=> ds :defaults {"db/id" 123 "user/name" "Joe"}} nil)
             (template {:=> ds :defaults {'db/id 123 'user/name "Joe"}} nil))))
    (testing "options as meta"
      (is (= [[:db/add 123 :user/name "Joe"]]
             (template (with-meta ds {:defaults {:db/id 123 :user/name "Joe"}}) nil)
             (template (with-meta ds {:defaults {"db/id" 123 "user/name" "Joe"}}) nil)
             (template (with-meta ds {:defaults {'db/id 123 'user/name "Joe"}}) nil))))
    (testing "value fn"
      (is (= {:status "online"}
             (template {:status :<user/status>} {:user/status (constantly "online")})
             (template {:status :<user/status>} {"user/status" (constantly "online")})
             (template {:status :<user/status>} {'user/status (constantly "online")})
             (template {:=> {:status :<user/status>} :defaults {:user/status (constantly "online")}} nil)
             (template {:=> {:status :<user/status>} :defaults {"user/status" (constantly "online")}} nil)
             (template {:=> {:status :<user/status>} :defaults {'user/status (constantly "online")}} nil)
             (template {:=> {:status :<user/status>} :default (constantly "online")} nil))))
    (testing "throw?"
      (is (= [[:db/add '_ :user/name '_]]
             (template {:=> ds :default '_ :throw? true} nil)))
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Missing value for template variable"
           (template {:=> ds :throw? true} nil))))
    (testing "throw-on"
      (is (= [[:db/add "tempid" :user/name "***"]]
             (template {:=> ds :throw-on #{:db/id} :default "***" :defaults {:db/id "tempid"}} nil)))
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Missing value for template variable"
           (template {:=> ds :throw-on #{:db/id} :default "***"} nil))))))
