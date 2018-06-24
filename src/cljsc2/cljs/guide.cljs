(ns cljsc2.cljs.guide
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljs.core.async :refer [<! timeout chan alts! close!]]
   [cljsc2.cljs.material_ui :refer [ui-button ui-stepper ui-step ui-step-label ui-step-content]]
   [cljsc2.cljs.parinfer_wrap :refer [parinferize!]]
   [fulcro.client :as fc]
   [fulcro.client.dom :as dom]
   [fulcro.client.primitives :as prim :refer [defsc]]
   [goog.dom :as gdom]))

(def lesson
  {:timescale 2
   :changes [{:selections [{:anchor {:line 0 :ch 0} :head {:line 0 :ch 0}}]
              :dt 0}
             {:change {:from {:line 0 :ch 0}
                       :to {:line 0 :ch 0}
                       :text ["(execute-plans)"]
                       :origin "paste"}}
             {:selections [{:anchor {:line 0 :ch 14} :head {:line 0 :ch 14}}]
              :dt 1000}
             {:change {:from {:line 0 :ch 14}
                       :to {:line 0 :ch 14}
                       :text [" (build \"SCV\")"]
                       :origin "paste"}}]
   :init-value "",
   :last-time 1501268674807
   :recording? false})

(def vcr (atom {1 lesson}))

(defn apply-change
  [cm {:keys [text from to origin]}]
  (.replaceRange cm
                 (clj->js text)
                 (clj->js from)
                 (clj->js to)
                 origin))

(defn apply-selections
  [cm selections]
  (.setSelections cm (clj->js selections)))


(defn play-recording!
  [cm-map key-]
  (let [cm (:cm cm-map)]
    (when-let [stop-chan (get-in @vcr [key- :stop-chan])]
      (close! stop-chan))
    (when (seq (get-in @vcr [key- :changes]))
      (swap! vcr assoc-in [key- :stop-chan] (chan))
      (let [recording (get @vcr key-)
            timescale (get recording :timescale 1)
            loop? (get recording :loop? true)
            loop-delay (get recording :loop-delay 2000)
            element (.getWrapperElement cm)
            cursor (gdom/getElementByClass "CodeMirror-cursors" element)]
        (aset cursor "style" "visibility" "visible")
        (go-loop []
          (reset! ((:get-prev-state cm-map)) nil)
          (.setValue cm (:init-value recording))
          (loop [changes (:changes recording)]
            (when (seq changes)
              (let [{:keys [change selections dt] :as data} (first changes)
                    tchan (timeout (/ dt timescale))
                    stop-chan (:stop-chan recording)
                    [v c] (alts! [tchan stop-chan])]
                (when (not= c stop-chan)
                  (cond
                    change (apply-change cm change)
                    selections (apply-selections cm selections)
                    :else nil)
                  (recur (rest changes))))))
          (when loop?
            (let [tchan (timeout loop-delay)
                  stop-chan (:stop-chan recording)
                  [v c] (alts! [tchan stop-chan])]
              (when (not= c stop-chan)
                (recur)))))))))

(defn set-component [this step no node]
  (let [state (prim/get-state this)
        cm-map (:cm-map state)]
    (when (and node (not cm-map) (= step no))
      (let [cm-map (parinferize! node)]
        (prim/set-state! this (assoc state :cm-map cm-map))
        (play-recording! cm-map 1)))))

(defsc UiGuide [this _]
  {:initLocalState (fn [] {:step 0})}
  (let [state (prim/get-state this)
        step (:step state 0)]
    (dom/div
     (ui-stepper {:activeStep step
                  :orientation "vertical"}
                 (ui-step
                  {:key 0}
                  (ui-step-label "Execute plans")
                  (ui-step-content
                   (dom/textarea
                    {:ref (partial set-component this step 0)
                     :className ".form-control"})))
                 (ui-step
                  {:key 1}
                  (ui-step-label "Add plans")
                  (ui-step-content
                   (dom/textarea
                    {:ref (partial set-component this step 1)
                     :className ".form-control"}))))
     (ui-button {:disabled (not (> step 0))
                 :onClick (fn [_] (prim/set-state! this
                                                   (-> state
                                                       (dissoc :cm-map)
                                                       (assoc :step (dec step)))))}
                "Step back")
     (ui-button {:disabled (not (< step 1))
                 :onClick (fn [_] (prim/set-state! this
                                                   (-> state
                                                       (dissoc :cm-map)
                                                       (assoc :step (inc step)))))}
                "Next step"))))

(def ui-guide (prim/factory UiGuide))
