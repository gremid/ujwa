(ns ujwa.main
  "Experiments with TEI/XML markup."
  (:require [clojure.java.io :as io]
            [clojure.set :refer [union]]
            [clojure.string :as str]
            [ujwa.xml :as xml]))

(defn- transduce-with-event-stack
  "Create stateful transducers based on a stack representing the (nesting) XML
  element context for each transduced XML event.

  `sf` maps an element-start event to a value to be pushed on the stack. `f`
  yields the transduced value based on the current stack and an input event."
  ([sf f]
   (partial transduce-with-event-stack sf f))
  ([sf f rf]
   (let [stack (atom (list))]
     (fn
       ([] (rf))
       ([result] (rf result))
       ([result evt]
        (cond
          (evt :xml<) (rf result (f (swap! stack conj (sf evt)) evt))
          (evt :xml>) (let [r (rf result (f @stack evt))] (swap! stack pop) r)
          :else (rf result (f @stack evt))))))))

(defn- parse-wit
  "Parses a list of witness sigils, separated by whitespace, into a set."
  [wit]
  (->> (-> wit (or "") str/trim (str/split #"\s+"))
       (map #(str/replace % #"^#" ""))
       (remove #{""})
       (apply hash-set)))

(def assoc-wit
  "Transducer associating the current witness with each event."
  (transduce-with-event-stack
   ;; put witness sets on the stack for <lem/> and <rdg/>
   (fn [{:keys [xml< wit]}]
     (if (#{"lem" "rdg"} xml<) (parse-wit wit)))
   ;; associate the closest witness set from the stack with each event
   (fn
     [stack evt]
     (assoc evt :wit (or (->> stack (remove nil?) (first)) #{})))))

(defn wit?
  "Predicate for events belonging to witness `w`."
  ([w]
   (partial wit? w))
  ([w {:keys [wit]}]
   (or (empty? wit) (wit w))))

(def assoc-masked?
  "Transducer masking events based on element context (header, paratext etc.)."
  (transduce-with-event-stack
   (fn [{:keys [xml<]}] (#{"teiHeader" "del" "note" "ref"} xml<))
   (fn
     [stack evt]
     (assoc evt :masked? (some some? stack)))))

(def remove-masked
  "Transducer removing masked events."
  (comp assoc-masked? (remove :masked?)))

(def assoc-container?
  "Transducer marking events in container elements."
  (transduce-with-event-stack
   (fn [{:keys [xml<]}] (#{"TEI" "text" "body" "app" "subst"} xml<))
   (fn [stack evt]
     (assoc evt :container? (-> stack first some?)))))

(defn whitespace?
  "Predicate for whitespace-only XML text events."
  [{:keys [xml!]}]
  (if xml! (re-seq #"^\s+$" xml!)))

(def remove-container-whitespace
  "Transducer removing whitespace-only events in container elements."
  (comp assoc-container? (remove #(and (:container? %) (whitespace? %)))))

(defn event->text
  "Extracts the text content of an event."
  [{:keys [xml< xml> xml! rend break]}]
  (cond
    (= xml< "p") "\n¶"
    (= xml< "lb") (if (or (= "no" break) (= "#hyphen" rend)) "" " ")
    (= xml< "pb") " "
    xml! (str/replace xml! #"\s+" " ")))

(defn events->text
  "Extracts the text content of a seq of events."
  [events]
  (-> (map event->text events)
      (str/join) (str/replace #"[ \t]+" " ") (str/trim)))

(defn with-xml-events
  "Calls a `f` with XML events from a given `file`."
  [file f]
  (with-open [is (-> file io/file io/input-stream)
              event-reader (xml/xml-event-reader is)]
    (f (xml/read-xml-events event-reader))))

(defn read-wits
  "Extracts all witness sigils from a seq of events."
  [events]
  (->> events (sequence assoc-wit) (map :wit) (apply union) (apply sorted-set)))

(defn extract-wit
  "Extracts events of a given witness `w` from a seq of events."
  [w events]
  (into
   []
   (comp assoc-wit (filter (wit? w)) remove-container-whitespace remove-masked)
   events))

(defn -main
  "Extracts all witnesses in plain text and as XML from a given file `f`.

  The results are written to files with paths `data/{sigil}.{xml|txt}`."
  [f]
  (doseq [wit (with-xml-events f read-wits)]
    (let [events (with-xml-events f (partial extract-wit wit))
          data-os #(->> (str wit "." %) (io/file "data") (io/output-stream))]
      (with-open [wit-xml (data-os "xml")
                  wit-txt (data-os "txt")
                  event-writer (xml/xml-event-writer wit-xml)
                  txt-writer (io/writer wit-txt :encoding "UTF-8")]
        (xml/write-xml-events event-writer events)
        (.write txt-writer (events->text events))))))
