(ns web-clojure.core
  (:require [compojure.core :as c]
            [ring.adapter.jetty :as j]
            [hiccup.core :as h]
            [ring.middleware.params :as p]
            [ring.util.response :as r])
  (:gen-class))

(defonce server (atom nil))
(defonce authenticodes (atom []))

(c/defroutes app
  (c/GET "/" []
    (h/html [:html]
            [:body
             [:form {:action "/add-authenticode" :method "post"}
              [:input {:type "text" :placeholder "Enter Code" :name "authenticode"}
               [:input {:type "text" :placeholder "Log Sender" :name "sender"}]
               [:button {:type "submit"} "Check Code"]]
              [:ol
               (map (fn [authenticode] (get authenticode :authenticode)
                      [:li authenticode (get authenticode :authenticode)])
                 @authenticodes)]]]))
  
  (c/POST "/add-authenticode" request
    (let [params (get request :params)
          authenticode (get params "authenticode")
          sender (get params "sender")
          authenticodeRecord (zipmap [:authenticode :sender] [authenticode sender])]
      (swap! authenticodes conj authenticodeRecord)
      (spit "authenticodes.edn" (pr-str @authenticodes))
      (r/redirect "/"))))

(def factorial (reductions * 1 (drop 1 (range))))

(defn factoradic [n] {:pre [(>= n 0)]}
   (loop [a (list 0) n n p 2]
      (if (zero? n) a (recur (conj a (mod n p)) (quot n p) (inc p)))))

(defn nth-permutation [s n] {:pre [(< n (nth factorial (count s)))]}
  (let [d (factoradic n)
        choices (concat (repeat (- (count s) (count d)) 0) d)]
    ((reduce 
        (fn [m i] 
          (let [[left [item & right]] (split-at i (m :rem))]
            (assoc m :rem (concat left right) 
                     :acc (conj (m :acc) item))))
      {:rem s :acc []} choices) :acc)))

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
