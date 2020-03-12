(ns ujwa.xml
  "XML stream processing functions."
  (:import [java.io InputStream OutputStream]
           [javax.xml.stream XMLEventReader XMLEventWriter XMLInputFactory XMLOutputFactory]
           [javax.xml.stream.events Attribute Characters EndElement StartElement XMLEvent]
           [javax.xml.transform.stream StreamResult StreamSource]))

(defn- xml-start-name
  [^StartElement event]
  (.. event getName getLocalPart))

(defn- xml-end-name
  [^EndElement event]
  (.. event getName getLocalPart))

(defn- xml-attrs
  [^StartElement event]
  (->>
   (for [^Attribute attr (iterator-seq (.getAttributes event))]
     [(keyword (.. attr getName getLocalPart)) (.getValue attr)])
   (into (hash-map))))

(defn- xml-text
  [^Characters event]
  (.getData event))

(defn- xml-event
  [^XMLEvent event]
  (merge
   {::evt event}
   (cond
     (.isStartElement event) (merge {:xml< (xml-start-name event)}
                                    (xml-attrs event))
     (.isEndElement event) {:xml> (xml-end-name event)}
     (.isCharacters event) {:xml! (xml-text event)})))

(defn ^XMLEventReader xml-event-reader
  [^InputStream is]
  (.. (XMLInputFactory/newInstance) (createXMLEventReader (StreamSource. is))))

(defn ^XMLEventWriter xml-event-writer
  [^OutputStream os]
  (.. (XMLOutputFactory/newInstance) (createXMLEventWriter (StreamResult. os))))

(defn read-xml-events
  [^XMLEventReader events]
  (->> (iterator-seq events) (map xml-event)))

(defn write-xml-events [^XMLEventWriter writer events]
  (doseq [^XMLEvent e (map ::evt events)] (.add writer e)))
