(ns cljsc2.cljs.dom_trash_drawer)

(defn find-or-create-node [id]
  (if (js/document.querySelector (str "[id='" id "']"))
    (js/document.querySelector (str "[id='" id "']"))
    (let [new-child (js/document.createElement "div")]
      (.add (.-classList new-child) "cljsc2-cell")
      (.setAttribute new-child "id" (str "[id='" id "']"))
      new-child)))
