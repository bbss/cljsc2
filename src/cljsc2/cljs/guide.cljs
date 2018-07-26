(ns cljsc2.cljs.guide
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljs.core.async :refer [<! timeout chan alts! close!]]
   [cljsc2.cljs.material_ui :refer [ui-button ui-paper ui-stepper ui-step
                                    ui-step-label ui-step-content ui-input
                                    ui-portal]]
   [cljsc2.cljs.ui.fulcro :refer [ui-input-with-label]]
   [cljsc2.cljs.dom_trash_drawer :refer [find-or-create-node]]
   [cljsc2.cljs.parinfer_wrap :refer [parinferize-cm!]]
   [fulcro.client :as fc]
   [fulcro.client.dom :as dom]
   [fulcro.client.primitives :as prim :refer [defsc]]
   [fulcro.ui.form-state :as fs]
   [goog.dom :as gdom]
   [react-dom :as react-dom]))

(defn parse-pos
  [pos]
  {:editor-caret/line (.-line pos)
   :editor-caret/ch (.-ch pos)
   :db/id (prim/tempid)})

(defn parse-change
  [change]
  {:editor-text-change/from (parse-pos (.-from change))
   :editor-text-change/to (parse-pos (.-to change))
   :editor-text-change/text (vec (seq (.-text change)))
   :editor-text-change/origin (.-origin change)
   :db/id (prim/tempid)})

(defn parse-selection
  [selection]
  {:editor-selection/anchor (parse-pos (.-anchor selection))
   :editor-selection/head (parse-pos (.-head selection))
   :db/id (prim/tempid)})

(defn parse-selections
  [selections]
  (map parse-selection selections))

(defsc EditorCaret [this _]
  {:query [:editor-caret/line :editor-caret/ch :db/id]
   :ident [:editor-caret/by-id :db/id]})

(defsc EditorSelection [this _]
  {:query [{:editor-selection/anchor (prim/get-query EditorCaret)}
           {:editor-selection/head   (prim/get-query EditorCaret)}
           :db/id]
   :ident [:editor-selection/by-id :db/id]})

(defsc EditorTextChange [this _]
  {:query [{:editor-text-change/from (prim/get-query EditorCaret)}
           {:editor-text-change/to (prim/get-query EditorCaret)}
           :editor-text-change/text
           :editor-text-change/origin
           :db/id]
   :ident [:editor-text-change/by-id :db/id]})

(defsc EditorChange [this _]
  {:query [:editor-change/dt
           {:editor-change/selections (prim/get-query EditorSelection)}
           {:editor-change/change (prim/get-query EditorTextChange)}
           :db/id]
   :ident [:editor-change/by-id :db/id]})

(defsc Step [this
             {:keys [:step/description
                     :step/explanation
                     :step/title
                     :step/timescale
                     :step/changes
                     :step/init-value
                     :step/last-time
                     :step/recording?
                     :step/wrap-dom-node
                     :db/id] :as step}
             {:keys [ui/editting]}]
  {:query [:step/description
           :step/explanation
           :step/timescale
           :step/title
           {:step/changes (prim/get-query EditorChange)}
           :step/init-value
           :step/last-time
           :step/recording?
           :step/wrap-dom-node
           :db/id
           fs/form-config-join]
   :ident [:step/by-id :db/id]
   :form-fields #{:step/description :step/explanation :step/init-value
                  :step/title}}
  (ui-paper (if editting
              (dom/span
               (ui-input-with-label
                this
                :step/title
                :step/title
                ""
                {:value title
                 :multiline true
                 :inputRef #(when js/window.Jupyter
                              (js/window.Jupyter.keyboard_manager.register_events %))
                 :onBlur #(prim/transact! this `[(fs/mark-complete! ~{:entity-id [:step/by-id id]})])
                 :placeholder "step title"})
               (ui-input-with-label
                this
                :step/description
                :step/description
                ""
                {:value description
                 :multiline true
                 :inputRef #(when js/window.Jupyter
                              (js/window.Jupyter.keyboard_manager.register_events %))
                 :onBlur #(prim/transact! this `[(fs/mark-complete! ~{:entity-id [:step/by-id id]})])
                 :placeholder "step description"})
               (ui-input-with-label
                this
                :step/explanation
                :step/explanation
                ""
                {:value explanation
                 :multiline true
                 :inputRef #(when js/window.Jupyter
                              (js/window.Jupyter.keyboard_manager.register_events %))
                 :onBlur #(prim/transact! this `[(fs/mark-complete! ~{:entity-id [:step/by-id id]})])
                 :placeholder "step explanation"})))))

(def ui-step-edittable (prim/factory Step))

(defsc Lesson [this {:keys [lesson/steps]}]
  {:query [{:lesson/steps (prim/get-query Step)}
           :db/id]
   :ident [:lesson/by-id :db/id]})

(def vcr (atom {}))

(defn apply-change
  [cm {:keys [editor-text-change/text editor-text-change/from
              editor-text-change/to editor-text-change/origin] :as d}]
  (.replaceRange cm
                 (clj->js text)
                 (clj->js from)
                 (clj->js to)
                 origin))

(defn apply-selections
  [cm selections]
  (.setSelections cm (clj->js selections)))

(defn cancel-playback! [{:keys [playing]}]
  (when playing (close! playing)))

(defn play-step!
  [cm-map step finished-cb]
  (do
    (when-let [stop-chan (get-in @vcr [(:db/id step) :stop-chan])]
      (close! stop-chan))
    (swap! vcr assoc-in [(:db/id step) :stop-chan] (chan))
    (let [stop-chan (get-in @vcr [(:db/id step) :stop-chan])
          timescale (get step :step/timescale 2)
          loop? (get step :step/loop? true)
          loop-delay (get step :step/loop-delay 2)
          element (.getWrapperElement (:cm cm-map))
          cursor (gdom/getElementByClass "CodeMirror-cursors" element)]
      (aset cursor "style" "visibility" "visible")
      (go-loop []
        (reset! ((:get-prev-state cm-map)) nil)
        (.setValue (:cm cm-map) (:step/init-value step))
        (loop [changes (:step/changes step)]
          (if (seq changes)
            (let [{:keys [editor-change/change editor-change/selections
                          editor-change/dt] :as data} (first changes)
                  tchan (timeout (/ dt timescale))
                  [v c] (alts! [tchan stop-chan])]
              (when (not= c stop-chan)
                (cond
                  change (apply-change (:cm cm-map) change)
                  selections (apply-selections (:cm cm-map) selections)
                  :else nil)
                (recur (rest changes))))
            (finished-cb)))
        (when loop?
          (let [tchan (timeout loop-delay)
                [v c] (alts! [tchan stop-chan])]
            (when (not= c stop-chan)
              (recur)))))
      stop-chan)))

(defn start-step-evaluation [this cm-map step step-no]
  (fn [_]
    (cancel-playback! cm-map)
    (prim/set-state! this (merge (prim/get-state this)
                                 {:started-eval-step step-no}))
    (play-step! cm-map
                (merge step {:step/loop? false
                             :step/timescale 100})
                #(.execute (:cell-node cm-map)))))

(defn stepper [this-of-guide cells steps {:keys [step/wrap-dom-node]
                                 :as step} editting-step cm-map]
  (let [{:keys [started-eval-step] :as state} (prim/get-state this-of-guide)
        {:keys []} (prim/get-computed this-of-guide)]
    (dom/div
     (apply ui-stepper
            (concat [{:activeStep step
                      :orientation "vertical"
                      :connector (dom/span)}]
                    (map-indexed
                     (fn [i {:keys [step/description step/explanation step/title] :as step-data}]
                       (let [first-selected? (= step 0)
                             is-selected (= step i)
                             is-near-selected (= (js/Math.abs (- i step)) 1)]
                         (if (or is-selected is-near-selected)
                           (if (= editting-step i)
                             (ui-step {:key i}
                                      (ui-step-label title)
                                      (ui-step-content description)
                                      (when started-eval-step (ui-step-content explanation))
                                      (ui-step-edittable
                                       (prim/computed step-data
                                                      {:ui/editting (= editting-step i)})))
                             (ui-step {:key i}
                                      (ui-step-label title)
                                      (ui-step-content description)))
                           (ui-step {:key i
                                     :style {:display "none"}}))))
                     steps)))
     (ui-button {:enabled (str (> step 0))
                 :onClick #(prim/transact!
                            this-of-guide
                            `[(cljsc2.cljc.mutations/step-to ~{:to (dec step)})])}
                "Step back")
     (when cm-map
       (ui-button {:disabled (not (nil? started-eval-step))
                   :onClick (start-step-evaluation this-of-guide cm-map (get steps step) step)}
                  "Eval step"))
     (ui-button {:disabled (or (>= step
                                   (dec (count steps)))
                               (and cm-map (not (= started-eval-step step))))
                 :onClick (fn [_]
                            (if (get-in steps [(inc step) :step/wrap-dom-node])
                              (do (js/Jupyter.notebook.insert_cell_at_index "code" 0)
                                  (js/setTimeout #(do (prim/transact!
                                                       this-of-guide
                                                       `[(cljsc2.cljc.mutations/step-to ~{:to (inc step)})])
                                                      (.setState
                                                       this-of-guide
                                                       (fn [s]
                                                         #js {"fulcro$state" (dissoc (aget s "fulcro$state")
                                                                                     :started-eval-step)})))
                                                 600))
                              (prim/transact!
                               this-of-guide
                               `[(cljsc2.cljc.mutations/step-to ~{:to (inc step)})])))}
                "Next step"))))

(defn connect-dom-node [this container-parent container-node]
  (do (.prepend (aget container-parent 0)
                container-node)
      (prim/set-state! this (merge (prim/get-state this)
                                   {:container-node container-node}))))

(defn add-playing [this cm-map]
  (assoc cm-map :playing
         (play-step! cm-map
                     (prim/props this)
                     (fn []))))

(defn on-cm-focus [this]
  (fn []
    (let [cm-map (:cm-map (prim/get-state this))]
      (cancel-playback! cm-map)
      (let [on-blur (fn []
                      (prim/set-state!
                       this
                       (merge (prim/get-state this)
                              {:cm-map (add-playing this cm-map)}))
                      (.off (:cm cm-map) "blur" (.-callee (cljs.core/js-arguments))))]
        (.on (:cm cm-map) "blur" on-blur)))))

(def frame-updates (atom {}))

(defn set-frame-updated! [step-id value]
  (swap! frame-updates assoc-in [step-id :frame-updated?] value))

(defn frame-updated? [step-id]
  (get-in @frame-updates [step-id :frame-updated?]))

(defn record-change! [step-id cm change]
  (let [data (get @vcr step-id)]
    (let [last-time (:step/last-time data)
          now (.getTime (js/Date.))
          dt (if last-time (- now last-time) 0)
          new-changes (vec (conj (:step/changes data) (->
                                                       change
                                                       (assoc :editor-change/dt dt)
                                                       (assoc :db/id (prim/tempid)))))
          new-data (assoc data
                          :step/last-time now
                          :step/changes new-changes)]
      (swap! vcr assoc step-id new-data))))

(defn on-change
  "Called after any change is applied to the editor."
  [step-id]
  (fn [cm change]
    (when (not= "setValue" (.-origin change))
      (record-change! step-id cm {:editor-change/change (parse-change change)})
      (set-frame-updated! cm true))))

(defn on-cursor-activity
  "Called after the cursor moves in the editor."
  [step-id]
  (fn [cm]
    (record-change! step-id cm {:editor-change/selections (vec (parse-selections (.listSelections cm)))})
    (set-frame-updated! cm false)))

(defn before-change
  "Called before any change is applied to the editor."
  [cm change]
  ;; keep CodeMirror from reacting to a change from "setValue"
  ;; if it is not a new value.
  (when (and (= "setValue" (.-origin change))
             (= (.getValue cm) (clojure.string/join "\n" (.-text change))))
    (.cancel change)))

(def RECORDING-AVAILABLE? true)

(defn setup-codemirror-focus-blur! [this cm]
  (.on cm "focus" (on-cm-focus this)))

(defn setup-codemirror! [this step-id cm]
  (.on cm "change" (on-change step-id))
  (.on cm "beforeChange" before-change)
  (.on cm "cursorActivity" (on-cursor-activity step-id)))

(defn remove-ids [tree]
  (clojure.walk/postwalk
   (fn [it] (if (= (type it)
                   fulcro.tempid/TempId)
              (str it)
              it))
   tree))

(defsc WrappedStep [this _ {:keys [steps step this-of-guide editting-step
                                   cells/jupyter-cells]}]
  {:componentDidMount (fn []
                        (let [{:keys [cells/jupyter-cells steps
                                      step editting-step]} (prim/get-computed this)
                              cell (first jupyter-cells)
                              jupyter-node (:cell/jupyter-node cell)
                              container-parent (.-element jupyter-node)
                              container-node (find-or-create-node (str "guide-wrapper-" (:db/id cell)))
                              _ (connect-dom-node this container-parent container-node)
                              cm-map (parinferize-cm! (.-code_mirror jupyter-node))]
                          (let [cm-map (if (= step editting-step)
                                         (do (setup-codemirror! this (:db/id (get steps step)) (:cm cm-map))
                                             cm-map)
                                         (add-playing this cm-map))]
                            (.focus_cell jupyter-node)
                            (cancel-playback! (:cm-map (prim/get-state this)))
                            (setup-codemirror-focus-blur! this (:cm cm-map))
                            (prim/set-state! this {:cm-map (assoc cm-map :cell-node jupyter-node)
                                                   :container-node container-node}))))
   :componentDidUpdate (fn [next-props next-state]
                         (when (not (= next-props
                                       (prim/props this)))
                           (let [{:keys [cells/jupyter-cells this-of-guide
                                         steps step editting-step]} (prim/get-computed this)
                                 cell (first jupyter-cells)
                                 jupyter-node (:cell/jupyter-node cell)
                                 container-parent (.-element jupyter-node)
                                 container-node (find-or-create-node (str "guide-wrapper-" (:db/id cell)))
                                 _ (connect-dom-node this container-parent container-node)
                                 cm-map (parinferize-cm! (.-code_mirror jupyter-node))]
                             (let [cm-map (if (= step editting-step)
                                            (do (setup-codemirror! this (:db/id (get steps step)) (:cm cm-map))
                                                cm-map)
                                            (if (:started-eval-step next-state)
                                              (add-playing this cm-map)
                                              cm-map))]
                               (.focus_cell jupyter-node)
                               (cancel-playback! (:cm-map (prim/get-state this)))
                               (setup-codemirror-focus-blur! this (:cm cm-map))
                               (prim/set-state! this {:cm-map (assoc cm-map :cell-node jupyter-node)
                                                      :container-node container-node})
                               (prim/set-state!
                                this-of-guide
                                (dissoc (prim/get-state this-of-guide) :started-eval-step))))))
   :componentWillUnmount (fn []
                           (let [{:keys [cells/jupyter-cells this-of-guide steps step]} (prim/get-computed this)
                                 cell (first jupyter-cells)
                                 jupyter-node (:cell/jupyter-node cell)
                                 container-parent (.-element jupyter-node)]
                             (cancel-playback! (:cm-map (prim/get-state this)))))}
  (ui-portal {:container (:container-node (prim/get-state this))}
             (dom/span
              (when RECORDING-AVAILABLE?
                (if editting-step
                  (dom/span (ui-button {:onClick #(do (reset! vcr {}))}
                                       "reset vcr")
                            (ui-button {:onClick #(do
                                                    (prim/set-state! this-of-guide {:editting-step nil})
                                                    (prim/transact! this `[(cljsc2.cljc.mutations/update-step-playback
                                                                            ~{:step-id (:db/id (get steps step))
                                                                              :step (merge (get steps step)
                                                                                           (get @vcr (:db/id (get steps step))))})])
                                                    (js/Jupyter.notebook.delete_cell
                                                     (aget (js/Jupyter.notebook.get_selected_cells_indices) 0)))}
                                       "finish editting"))
                  (dom/span (ui-button
                             {:onClick
                              #(do (prim/set-state! this-of-guide {:editting-step step})
                                   (prim/transact!
                                    this `[(cljsc2.cljc.mutations/add-new-step
                                            ~{:at-index step})]))}
                             "add new step")
                            (ui-button
                             {:onClick
                              #(do (reset! vcr {})
                                   (prim/set-state! this-of-guide {:editting-step step})
                                   (prim/transact!
                                    this `[(cljsc2.cljc.mutations/add-new-step
                                            ~{:at-index step
                                              :wrap-dom-node :code-cell})]))}
                             "add new code step")
                            (ui-button {:onClick #(cljs.pprint/pprint (remove-ids (get steps step)))}
                                       "print step data")
                            (ui-button {:onClick #(prim/set-state! this-of-guide {:editting-step step})}
                                       "edit this step"))))
              (stepper this-of-guide (:cells/jupyter-cells (prim/get-computed this))
                       steps step editting-step (:cm-map (prim/get-state this))))))

(def ui-wrapped-step (prim/factory WrappedStep))

(defsc UiGuide [this {:keys [:ui-guide/lesson
                             :ui/step]} {:keys [cells/jupyter-cells]}]
  {:query [{:ui-guide/lesson (prim/get-query Lesson)} :ui/step :db/id]
   :ident [:ui-guide/by-id :db/id]
   :initLocalState (fn [] {:editting-step nil})}
  (let [{:keys [lesson/steps]} lesson
        {:keys [step/wrap-dom-node] :as selected-step} (get steps step)
        {:keys [editting-step] :as state} (prim/get-state this)]
    (ui-paper
     {:style {:margin 10}}
     (if wrap-dom-node
       (ui-wrapped-step (prim/computed
                         (get steps step)
                         (merge {:step step :steps steps :this-of-guide this}
                                {:cells/jupyter-cells jupyter-cells
                                 :editting-step editting-step :local-state (prim/get-state this)})))
       (dom/span
        (when RECORDING-AVAILABLE?
          (if editting-step
            (ui-button {:onClick #(prim/set-state! this {:editting-step nil})}
                       "finish editting")
            (dom/span
             (ui-button {:onClick #(do (prim/set-state! this {:editting-step step})
                                       (prim/transact! this `[(cljsc2.cljc.mutations/add-new-step ~{:at-index step})]))}
                        "add new step")
             (ui-button {:onClick #(do (prim/set-state! this {:editting-step step})
                                       (prim/transact! this `[(cljsc2.cljc.mutations/add-new-step ~{:at-index step
                                                                                                    :wrap-dom-node :code-cell})]))}
                        "add new code step"))))
        (stepper this jupyter-cells steps step editting-step nil))))))

(def ui-guide (prim/factory UiGuide))
