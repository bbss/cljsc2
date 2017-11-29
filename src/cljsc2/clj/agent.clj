(ns cljsc2.clj.agent)

(defn distance [[x1 y1] [x2 y2]]
  (let [dx (- x2 x1), dy (- y2 y1)]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(defn move-to-action [[tag [x y]]]
  #:SC2APIProtocol.sc2api$Action
  {:action-raw #:SC2APIProtocol.raw$ActionRaw
   {:action #:SC2APIProtocol.raw$ActionRaw
    {:unit-command #:SC2APIProtocol.raw$ActionRawUnitCommand
     {:unit-tags [tag]
      :ability-id 16
      :target #:SC2APIProtocol.raw$ActionRawUnitCommand
      {:target-world-space-pos #:SC2APIProtocol.common$Point2D
       {:x x
        :y y}}}}}})

(defonce training-data-collection (atom {}))

(defn sub-step [observation connection]
  ;;marines should collect minerals as fast as they can
  ;;each marine can follow a seperate route
  ;;a marine is a controllable unit
  ;;to control him (in this case by moving) we need to select him
  ;;collecting is done by moving over the mineral
  ;;so we need to figure out the closest mineral to each marine and send them to that location
  (let [units (->> observation
                   :raw-data
                   :units)
        marines  (filter (comp #{48} :unit-type)
                         units)
        minerals (filter (comp #{1680} :unit-type)
                         units)
        all-distances (for [marine marines
                            mineral minerals]
                        [(distance ((comp (juxt :x :y) :pos) marine)
                                   ((comp (juxt :x :y) :pos) mineral))
                         marine
                         mineral])
        grouped-by-marine (group-by (comp :tag second) all-distances)
        unit-tags-to-coords (->> grouped-by-marine
                                 (map (fn [[tag distance-marine-mineral]]
                                        (let [closest-mineral-coords (nth (apply min-key first distance-marine-mineral) 2)]
                                          [tag ((comp (juxt :x :y) :pos) closest-mineral-coords)])))
                                 )]
    (doall (map-indexed (fn [i [_ coords]]
                          (swap! training-data-collection
                                 assoc
                                 (->> observation
                                      :feature-layer-data
                                      :renders
                                      :unit-type
                                      :data)
                                 [i coords]))
                        unit-tags-to-coords))
    (if (empty? unit-tags-to-coords)
      (println "empty")
      (map move-to-action unit-tags-to-coords))
    ))

(defn step [observation connection]
  (def obs observation)
  (try
    (sub-step observation connection)
    (catch Exception e [])))
