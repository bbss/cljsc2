(ns cljsc2.cljs.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [react :as react]
            [d3 :as d3]
            [clojure.core.async :refer [<! >!]]
            cljsc2.cljs.pushhandlers
            cljsc2.cljc.mutations
            [cljsc2.cljs.ui.player_setup :refer [PlayerSetup ui-player-setup]]
            [cljsc2.cljs.selectors :refer [latest-observation-from-runs]]
            [cljsc2.cljs.ui.canvas_drawing :refer [render-screen render-minimap]]
            [cljsc2.cljs.ui.run :refer [ui-run-config Run RunConfig]]
            [cljsc2.cljs.ui.fulcro :refer [input-with-label]]
            [cljsc2.cljs.guide :refer [ui-guide]]
            [cljsc2.cljs.material_ui :refer [ui-button ui-paper]]
            [cljsc2.cljs.ui.process :refer [Process ui-process ProcessStarter ui-process-starter]]
            [cljsc2.cljs.ui.player_setup]
            [cljsc2.cljc.model :as model]
            [datascript.transit :as dst]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]
            [oops.core :refer [oset! oget]]
            [goog.dom :as gdom]
            [taoensso.sente  :as sente :refer (cb-success?)]
            [taoensso.sente.packers.transit :as sente-transit]
            [cljs.spec.alpha :as spec]
            [fulcro.client :as fc]
            [fulcro.client.dom :as dom]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.ui.form-state :as fs]
            [fulcro.client.data-fetch :refer [load] :as df]
            [fulcro.websockets :as fw]
            [fulcro.websockets.networking :refer [push-received]]))

(reset! sente/debug-mode?_ true)

(enable-console-print!)

(def pri js/console.log)

(defsc Root [this {:keys [:root/processes :root/process-starter :root/starcraft-static-data]}]
  {:query [{:root/processes (prim/get-query Process)}
           {:root/process-starter (prim/get-query ProcessStarter)}
           :root/starcraft-static-data]}
  (ui-paper {:style {:margin 10}}
            (ui-guide)
           (when (empty? processes)
             (dom/h3 "There are no starcraft processes running yet, they will start automatically when you run a code cell. (play button or shortcut ctrl-enter)"))
           (when (and (not (empty? process-starter)) (not (seq processes)))
             (ui-process-starter (prim/computed process-starter {:processes processes})))
           (map #(ui-process (prim/computed % {:knowledge-base starcraft-static-data}))
                processes)))

(defonce app (atom (fc/new-fulcro-client)))

(defn mount []
  (reset! app (fc/mount @app Root "sc-viewer")))

(defn reset-app! []
  (reset!
   app
   (fc/new-fulcro-client
    :networking {:remote
                 (fw/make-websocket-networking
                  {:host (case :remote-staging
                           :local "0.0.0.0:3446"
                           :local-staging "192.168.1.94:3446"
                           :remote-staging "cljsc.org"
                           (pri "no ip for env"))
                   :push-handler (fn [m]
                                   (push-received @app m))
                   :transit-handlers {:read (merge {"literal-byte-string" (fn [it] it)}
                                                   datascript.transit/read-handlers)}
                   })}
    :started-callback (fn [app]
                        (do (load app ::model/processes Process
                                  {:target [:root/processes]
                                   :marker false})
                            (load app ::model/process-starter ProcessStarter
                                  {:target [:root/process-starter]
                                   :marker false})
                            (load app :root/starcraft-static-data Root
                                  {:marker false
                                   :post-mutation `cljsc2.cljc.mutations/make-conn}))))))

(defn init! []
  (println "init cljsc2 ui")
  (when-let [el (aget (.querySelectorAll js/document "#sc-viewer") 0)]
    (.remove el))
  (reset-app!)
  (let [el (js/document.createElement "div")
        _ (oset! el "id" "sc-viewer")]  (.prepend (gdom/getElement "notebook-container") el))
  (mount))

(defn ^:dev/after-load on-js-reload []
  (when-let [el (aget (.querySelectorAll js/document "#sc-viewer") 0)]
    (.remove el))
  (let [el (js/document.createElement "div")
        _ (oset! el "id" "sc-viewer")]  (.prepend (gdom/getElement "notebook-container") el))
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
