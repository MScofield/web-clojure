(ns web-clojure.core
  (:require [compojure.core :as c]
            [ring.adapter.jetty :as j]
            [hiccup.core :as h]
            [ring.middleware.params :as p]
            [ring.util.response :as r])
  (:gen-class))

(defonce server (atom nil))
(defonce authenticodes (atom []))

(def factorial (reductions * 1 (drop 1 (range))))
(def colours ["-red-" "-blue-" "-green-" "-yellow-" "-cyan-" "-magenta-" "-black-" "-white-"])

(defn factoradic [n] {:pre [(>= n 0)]}
   (loop [a (list 0) n n p 2]
      (if (zero? n) a (recur (conj a (mod n p)) (quot n p) (inc p)))))

(defn nth-permutation [s n] (println s)(println n)
  {:pre [(< n (nth factorial (count s)))]}
  (let [d (factoradic n)
        choices (concat (repeat (- (count s) (count d)) 0) d)]
    ((reduce 
        (fn [m i] 
          (let [[left [item & right]] (split-at i (m :rem))]
            (assoc m :rem (concat left right) 
                     :acc (conj (m :acc) item))))
      {:rem s :acc []} choices) :acc)))

(c/defroutes app
  (c/GET "/" []
    (h/html [:html]
            [:body 
             [:h2 "Authenticate Your Important Emails with AuthentiCode!!!"]
             [:form {:action "/add-authenticode" :method "post"}
              [:input {:type "text" :placeholder "Enter AuthentiCode" :name "authenticode"}"\u00A0"
               [:input {:type "text" :placeholder "Log Sender" :name "sender"}]"\u00A0"
               [:input {type "text" :placeholder "Log Recipient" :name "recipient"}]"\u00A0" 
               [:button {:type "submit"} "Check Code"]] 
              [:hr]
              [:table (:style "border: 0; width: 90%")
               [:tr [:th "Code Log""\u00A0 \u00A0 \u00A0"][:th "Sender Log""\u00A0 \u00A0 \u00A0"][:th "Receiver Log""\u00A0\u00A0\u00A0"][:th "Authenticity Confirmation"]]
               [:td
                     (map (fn [authenticode] (get authenticode :authenticode)
                            (get authenticode :sender)
                            [:tr authenticode 
                             [:td (get authenticode :authenticode)]
                             [:td (get authenticode :sender)]
                             [:td (get authenticode :recipient)]
                             [:td (get authenticode :keyOrder)]])
                       @authenticodes)]]]]))
  
  (c/POST "/add-authenticode" request
    (let [params (get request :params)
          authenticode (Integer/parseInt (get params "authenticode"))
          sender (get params "sender")
          recipient (get params "recipient")
          keyOrder (remove nil? (nth-permutation colours authenticode))
          authenticodeRecord (zipmap [:authenticode :sender :recipient :keyOrder] [authenticode sender recipient keyOrder])]
      (swap! authenticodes conj authenticodeRecord)
      (spit "authenticodes.edn" (pr-str @authenticodes))
      (println (remove nil? (nth-permutation colours authenticode)))
      (r/redirect "/"))))

(defn -main []
  (try
    (let [authenticodes-str (slurp "authenticodes.edn")
          authenticodes-vec (read-string authenticodes-str)]
      (reset! authenticodes authenticodes-vec))
    (catch Exception _))
  (when @server
    (.stop @server))
  (let [app (p/wrap-params app)]
    (reset! server (j/run-jetty app {:port 3000 :join? false}))))
