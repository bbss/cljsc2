(ns cljsc2.cljs.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [react :as react]
            [react-dom :as react-dom]
            [clojure.core.async :refer [<! >!]]
            cljsc2.cljs.pushhandlers
            cljsc2.cljc.mutations
            [cljsc2.cljc.lesson :refer [lesson]]
            [cljsc2.cljs.guide :refer [ui-guide UiGuide]]
            [cljsc2.cljs.material_ui :refer [ui-button ui-paper ui-portal]]
            [cljsc2.cljs.ui.process :refer [Process ui-process ProcessStarter ui-process-starter]]
            [cljsc2.cljs.jupyter :refer [JupyterCells ui-jupyter-cell]]
            [cljsc2.cljc.model :as model]
            [oops.core :refer [oset! oget]]
            [goog.dom :as gdom]
            [taoensso.sente  :as sente]
            [datascript.transit :refer [read-handlers]]
            [fulcro.client :as fc]
            [fulcro.client.dom :as dom]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.data-fetch :refer [load]]
            [fulcro.websockets :as fw]
            [fulcro.websockets.networking :refer [push-received]]))

(defn ui-menu-button [this]
  (let [code-cell-node (.item (js/document.querySelectorAll "#maintoolbar-container") 0)]
    (when (and code-cell-node (.-lastChild code-cell-node))
      (let [has-child (.contains (.-classList (.-lastChild code-cell-node)) "cljsc2-cell")
            child (if has-child (.-lastChild code-cell-node) (let [new-child (js/document.createElement "div")]
                                                               (.add (.-classList new-child) "cljsc2-cell")
                                                               (.add (.-classList new-child) "btn-group")
                                                               (.append code-cell-node new-child)
                                                               new-child))]
        (ui-portal {:container child}
                   (ui-button {:style {:display "inline"}
                               :onClick (fn [_] (prim/set-state! this {:show (not (:show (prim/get-state this)))}))}
                              (if (:show (prim/get-state this)) "hide cljsc2 menu" "show cljsc2 menu")))))))

(defn add-on-click [cell-handlers cell]
  (prim/computed cell {:onClick (get cell-handlers (:db/id cell)
                                     (fn [] (.execute (:cell/jupyter-node cell))))}))

(defsc Root [this {:keys [:root/guide :root/processes :root/process-starter
                          :root/starcraft-static-data :root/jupyter-cells
                          :root/cell-handlers]}]
  {:query [{:root/processes (prim/get-query Process)}
           {:root/process-starter (prim/get-query ProcessStarter)}
           {:root/jupyter-cells (prim/get-query JupyterCells)}
           :root/starcraft-static-data
           {:root/guide (prim/get-query UiGuide)}
           :root/cell-handlers]
   :initLocalState (fn [] {:show false})}
  (dom/div
     (when (:show (prim/get-state this))
       (ui-paper
        {:style {:margin 10}}
        (when (and (not (empty? process-starter)) (not (seq processes)))
          (ui-process-starter (prim/computed process-starter {:processes processes})))
        (map #(ui-process (prim/computed % {:knowledge-base starcraft-static-data}))
             processes)))
     (when (seq guide)
       (ui-guide (prim/computed guide jupyter-cells)))
     (->> (:cells/jupyter-cells jupyter-cells)
          (map (partial add-on-click cell-handlers))
          (map ui-jupyter-cell))
     (ui-menu-button this)))

(defonce app (atom (fc/new-fulcro-client)))

(defn mount []
  (reset! app (fc/mount @app Root "sc-viewer")))

(defn code-cell-sync [app]
  (js/setInterval #(let [cells (js/Jupyter.notebook.get_cells)
                         ids (map (fn [cell] [:cell/by-id (.-cell-id cell)]) cells)]
                     (when (not= (.getAttribute (js/document.querySelector "#site") "style") "display: block;")
                       (.setAttribute (js/document.querySelector "#site") "style" "display: block;"))
                     (when (not (= ids
                                   (get-in @(prim/app-state (:reconciler app))
                                           [:cells/by-id 1 :cells/jupyter-cells])))
                       (prim/merge-component!
                        (:reconciler app)
                        JupyterCells
                        {:cells/jupyter-cells (into [] (map (fn [cell]
                                                              {:db/id (.-cell_id cell)
                                                               :cell/jupyter-node cell})
                                                            cells))
                         :db/id 1}
                        :replace [:root/jupyter-cells])))
                  500))

(defonce code-cell-syncer-id (atom 0))

(defn reset-code-cell-syncer [app]
  (js/clearInterval @code-cell-syncer-id)
  (reset! code-cell-syncer-id (code-cell-sync app)))

(defn reset-app! []
  (reset!
   app
   (fc/new-fulcro-client
    :networking {:remote
                 (fw/make-websocket-networking
                  {:host (case :local
                           :local "0.0.0.0:3446"
                           :local-staging "192.168.1.94:3446"
                           :remote-staging "cljsc.org"
                           (println "no ip for env"))
                   :push-handler (fn [m]
                                   (push-received @app m))
                   :transit-handlers {:read (merge {"literal-byte-string" (fn [it] it)}
                                                   read-handlers)}})}
    :started-callback (fn [app]
                        (prim/merge-component!
                         (:reconciler app)
                         UiGuide {:ui-guide/lesson lesson
                                  :db/id 1
                                  :ui/step 0} :replace [:root/guide])
                        (do (load app ::model/processes Process
                                  {:target [:root/processes]
                                   :marker false})
                            (load app ::model/process-starter ProcessStarter
                                  {:target [:root/process-starter]
                                   :marker false})
                            (load app :root/starcraft-static-data Root
                                  {:marker false
                                   :post-mutation `cljsc2.cljc.mutations/make-conn}))
                        (reset-code-cell-syncer app)))))


(defn init! []
  (js/setTimeout #(do
                   (when-let [el (aget (.querySelectorAll js/document "#sc-viewer") 0)]
                     (.remove el))
                   (reset-app!)
                   (let [el (js/document.createElement "div")
                         _ (oset! el "id" "sc-viewer")]  (.append (gdom/getElement "header") el))
                   (mount))
                 1000))

(defn ^:dev/after-load on-js-reload []
  (when-let [el (aget (.querySelectorAll js/document "#sc-viewer") 0)]
    (.remove el))
  (let [cells (.querySelectorAll js/document ".cljsc2-cell")
        amount (oget (.querySelectorAll js/document ".cljsc2-cell") "length")]
    (doall(for [i (range amount)]
            (.remove (aget cells i)))))
  (let [el (js/document.createElement "div")
        _ (oset! el "id" "sc-viewer")]  (.append (gdom/getElement "header") el))
  (go-loop [stopped (<! (:ch-recv @(:channel-socket (:remote (:networking @app)))))]
    (reset-app!)
    (mount))
  (sente/chsk-disconnect!
   (:chsk @(:channel-socket (:remote (:networking @app))))))


;;in notebook page during devtime.
;; <script type="text/javascript">
;; setInterval(function () {
;;                          [].forEach.call(document.querySelectorAll('.form-control'), i => Jupyter.keyboard_manager.register_events(i))
;;                          }, 1000)
;; </script>
