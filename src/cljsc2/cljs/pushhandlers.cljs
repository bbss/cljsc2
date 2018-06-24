(ns cljsc2.cljs.pushhandlers
  (:require [cljsc2.cljs.ui.run :refer [Run RunConfig]]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.ui.form-state :as fs]
            [fulcro.websockets :as fw]
            [fulcro.websockets.networking :refer [push-received]]))

(defmethod push-received :add-observation
  [{:keys [reconciler] :as app}
   {{:keys [run-id ident-path observation] :as msg} :msg}]
  (let [state (prim/app-state reconciler)
        observation (merge
                     observation
                     {:db/id (second ident-path)})]
    (swap! state (fn [s]
                   (-> s
                       (assoc-in ident-path observation)
                       (prim/integrate-ident ident-path
                                             :append [:run/by-id run-id :run/observations]))))))

(defmethod push-received :run-added
  [{:keys [reconciler] :as app} {{:keys [run process-id]} :msg}]
  (prim/merge-component! reconciler Run run
                         :append [:process/by-id process-id :process/runs]))

(defmethod push-received :savepoint-added
  [{:keys [reconciler] :as app} {{:keys [ident-path savepoint-at]} :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state assoc-in (conj ident-path :process/savepoint-at) savepoint-at)))

(defmethod push-received :run-config-added
  [{:keys [reconciler] :as app} {{:keys [run-config process-ident]} :msg}]
  (let [form-merged (fs/add-form-config RunConfig run-config)]
    (prim/merge-component! reconciler RunConfig form-merged)
    #_(prim/merge-component! reconciler Process {:db/id 1
                                                 :process/run-config run-config})
    (swap! (prim/app-state reconciler) (fn [s]
                                         (assoc-in s (conj process-ident :process/run-config) [:run-config/by-id (:db/id run-config)] )))))

(defmethod push-received :run-ended
  [{:keys [reconciler] :as app} {{:keys [ident-path ended-at]} :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state (fn [s]
                   (let [observation-ident-paths (-> (get-in s (conj ident-path :run/observations))
                                                     reverse
                                                     rest)]
                     (-> (reduce (fn [s [path key]]
                                   (update s path (fn [observations]
                                                    (dissoc observations key))))
                                 s observation-ident-paths)
                         (assoc-in (conj ident-path :run/ended-at) ended-at)
                         (update-in (conj ident-path :run/observations)
                                    (comp vector last))))))))

(defmethod push-received :run-started
  [{:keys [reconciler] :as app} {{:keys [ident-path started-at]} :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state assoc-in (conj ident-path :run/started-at) started-at)))

(defmethod push-received :process-spawned [{:keys [reconciler] :as app} {process :msg}]
  (let [state (prim/app-state reconciler)
        ident-path [:process/by-id (:db/id process)]]
    (swap! state (fn [s]
                   (-> s
                       (assoc-in ident-path process)
                       (prim/integrate-ident ident-path :append [:root/processes]))))))

(defmethod push-received :process-died [{:keys [reconciler] :as app} {process-ident-path :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state
           (fn [s]
             (-> s
                 (update :process/by-id (fn [processes]
                                          (dissoc
                                           processes
                                           (second process-ident-path))))
                 (update :root/processes (fn [processes]
                                           (vec (filter
                                                (comp not #{process-ident-path})
                                                processes)))))))))

(defmethod push-received :game-info-added [{:keys [reconciler] :as app}
                                           {{:keys [value ident-path]} :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state
           (fn [s]
             (-> s
                 (assoc-in (conj ident-path :process/game-info)  value))))))

(defmethod push-received :latest-response [{:keys [reconciler] :as app}
                                           {{:keys [response ident-path]} :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state
           (fn [s]
             (-> s
                 (assoc-in (conj ident-path :process/latest-response) response))))))
(defmethod push-received :add-observation
  [{:keys [reconciler] :as app}
   {{:keys [run-id ident-path observation] :as msg} :msg}]
  (let [state (prim/app-state reconciler)
        observation (merge
                     observation
                     {:db/id (second ident-path)})]
    (swap! state (fn [s]
                   (-> s
                       (assoc-in ident-path observation)
                       (prim/integrate-ident ident-path
                                             :append [:run/by-id run-id :run/observations]))))))

(defmethod push-received :run-added
  [{:keys [reconciler] :as app} {{:keys [run process-id]} :msg}]
  (prim/merge-component! reconciler Run run
                         :append [:process/by-id process-id :process/runs]))

(defmethod push-received :savepoint-added
  [{:keys [reconciler] :as app} {{:keys [ident-path savepoint-at]} :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state assoc-in (conj ident-path :process/savepoint-at) savepoint-at)))

(defmethod push-received :run-config-added
  [{:keys [reconciler] :as app} {{:keys [run-config process-ident]} :msg}]
  (let [form-merged (fs/add-form-config RunConfig run-config)]
    (prim/merge-component! reconciler RunConfig form-merged)
    #_(prim/merge-component! reconciler Process {:db/id 1
                                                 :process/run-config run-config})
    (swap! (prim/app-state reconciler) (fn [s]
                                         (assoc-in s (conj process-ident :process/run-config) [:run-config/by-id (:db/id run-config)] )))))

(defmethod push-received :run-ended
  [{:keys [reconciler] :as app} {{:keys [ident-path ended-at]} :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state (fn [s]
                   (let [observation-ident-paths (-> (get-in s (conj ident-path :run/observations))
                                                     reverse
                                                     rest)]
                     (-> (reduce (fn [s [path key]]
                                   (update s path (fn [observations]
                                                    (dissoc observations key))))
                                 s observation-ident-paths)
                         (assoc-in (conj ident-path :run/ended-at) ended-at)
                         (update-in (conj ident-path :run/observations)
                                    (comp vector last))))))))

(defmethod push-received :run-started
  [{:keys [reconciler] :as app} {{:keys [ident-path started-at]} :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state assoc-in (conj ident-path :run/started-at) started-at)))

(defmethod push-received :process-spawned [{:keys [reconciler] :as app} {process :msg}]
  (let [state (prim/app-state reconciler)
        ident-path [:process/by-id (:db/id process)]]
    (swap! state (fn [s]
                   (-> s
                       (assoc-in ident-path process)
                       (prim/integrate-ident ident-path :append [:root/processes]))))))

(defmethod push-received :process-died [{:keys [reconciler] :as app} {process-ident-path :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state
           (fn [s]
             (-> s
                 (update :process/by-id (fn [processes]
                                          (dissoc
                                           processes
                                           (second process-ident-path))))
                 (update :root/processes (fn [processes]
                                           (vec (filter
                                                (comp not #{process-ident-path})
                                                processes)))))))))

(defmethod push-received :game-info-added [{:keys [reconciler] :as app}
                                           {{:keys [value ident-path]} :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state
           (fn [s]
             (-> s
                 (assoc-in (conj ident-path :process/game-info)  value))))))

(defmethod push-received :latest-response [{:keys [reconciler] :as app}
                                           {{:keys [response ident-path]} :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state
           (fn [s]
             (-> s
                 (assoc-in (conj ident-path :process/latest-response) response))))))
