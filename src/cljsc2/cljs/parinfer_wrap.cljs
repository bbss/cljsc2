(ns cljsc2.cljs.parinfer_wrap
  (:require
   [parinfer :as parinfer]
   [parinfer-codemirror :as parinfer-codemirror]
   [codemirror :as codemirror]))

(def editor-opts
  {:mode "clojure-parinfer"
   :theme "github"
   :matchBrackets true})

(defn parinferize!
  ([textarea-node] (parinferize! textarea-node {}))
  ([textarea-node opts]
   (let [cm (codemirror/fromTextArea textarea-node (clj->js (merge editor-opts opts)))
         wrapper (.getWrapperElement cm)
         prev-editor-state (atom nil)]
     (.init parinfer-codemirror cm "smart" {:forceBalance true})
     #_(specify! cm
                 IEditor
                 (get-prev-state [this] prev-editor-state)
                 (cm-key [this] key-)
                 (frame-updated? [this] (get-in @frame-updates [key- :frame-updated?]))
                 (set-frame-updated! [this value] (swap! frame-updates assoc-in [key- :frame-updated?] value))
                 (record-change! [this new-thing]
                                 (let [data (get @vcr key-)]
                                   (when (:recording? data)
                                     (let [last-time (:last-time data)
                                           now (.getTime (js/Date.))
                                           dt (if last-time (- now last-time) 0)
                                           new-changes (conj (:changes data) (assoc new-thing :dt dt))
                                           new-data (assoc data
                                                           :last-time now
                                                           :changes new-changes)]
                                       (swap! vcr assoc key- new-data))))))
     {:cm cm
      :get-prev-state (fn [] prev-editor-state)})))
