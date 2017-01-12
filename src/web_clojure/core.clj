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
               [:button {:type "submit"} "Check Code"]]
              [:ol
               (map (fn [authenticode]
                      [:li authenticode])
                 @authenticodes)]]]))
  
  (c/POST "/add-authenticode" request
    (let [params (get request :params)
          authenticode (get params "authenticode")]
      (swap! authenticodes conj authenticode)
      (spit "authenticodes.edn" (pr-str @authenticodes))
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
