(ns cljsc2.cljs.jupyter
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljs.core.async :refer [<! timeout chan alts! close!]]
   [cljsc2.cljs.dom_trash_drawer :refer [find-or-create-node]]
   [cljsc2.cljs.material_ui :refer [ui-button ui-icon-button ui-paper ui-tooltip
                                    ui-stepper ui-step ui-step-label ui-step-content
                                    ui-portal create-element]]
   [cljsc2.cljs.parinfer_wrap :refer [parinferize!]]
   ["@material-ui/icons" :refer [PlayArrow]]
   [fulcro.client.dom :as dom]
   [fulcro.client.primitives :as prim :refer [defsc]]
   [goog.dom :as gdom]))

(def ui-play-arrow (create-element PlayArrow))

(defsc JupyterCell [this {:keys [db/id cell/jupyter-node]} {:keys [onClick]}]
  {:query [:cell/jupyter-node :db/id]
   :ident [:cell/by-id :db/id]
   :componentDidMount #(let [{:keys [db/id cell/jupyter-node]} (prim/props this)
                             container-node (find-or-create-node id)]
                         (prim/set-state! this {:container-node container-node})
                         (.prepend (.-element jupyter-node) container-node))}
  (ui-portal {:container (:container-node (prim/get-state this))}
             (ui-tooltip {:title "Run cell"
                          :enterDelay 300}
                         (ui-icon-button
                          {:onClick onClick}
                          (ui-play-arrow)))))

(def ui-jupyter-cell (prim/factory JupyterCell {:keyfn :db/id}))

(defsc JupyterCells [this _]
  {:query [{:cells/jupyter-cells (prim/get-query JupyterCell)}
           :db/id]
   :ident [:cells/by-id :db/id]})
