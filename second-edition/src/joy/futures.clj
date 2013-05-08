(ns joy.futures
  "Examples for futures from section 11.6"
  (:require (clojure [xml :as xml]))
  (:require (clojure [zip :as zip]))
  (:import  (java.util.regex Pattern)))

(defn normalize [feed]
  (if (= :feed (:tag (first feed)))
    feed
    (zip/down feed)))

(defmulti rss-children class)

(defmethod rss-children String [uri-str]
  (->> (xml/parse uri-str)
       zip/xml-zip
       normalize
       zip/children
       (filter (comp #{:item :entry} :tag))))

(defn title [entry]
  (some->> entry
           :content
           (some #(when (= :title (:tag %)) %))
           :content
           first))

(defn count-text-task [extractor txt feed]
  (let [items (rss-children feed)
        re    (Pattern/compile (str "(?i)" txt))]
    (->> items
         (map extractor)
         (mapcat #(re-seq re %))
         count)))

(defmacro as-futures [[a args] & body]
  (let [parts          (partition-by #{'=>} body)
        [acts _ [res]] (partition-by #{:as} (first parts))
        [_ _ task]     parts]
    `(let [~res (for [~a ~args] (future ~@acts))]
       ~@task)))

(defn occurrences [extractor tag & feeds]
  (as-futures [feed feeds]
     (count-text-task extractor tag feed)
     :as results
    =>
    (reduce (fn [total res] (+ total @res))
            0
            results)))

(comment

  (count-text-task
   title
   "Erlang"
   "http://feeds.feedburner.com/ElixirLang")
  ;;=> 0

  (count-text-task
   title
   "Elixir"
   "http://feeds.feedburner.com/ElixirLang")
  ;;=> 14

  (count-text-task
   title
   "Yak"
   "http://blog.fogus.me/feed/")
  ;;=> 1
  
  (occurrences content)

)
