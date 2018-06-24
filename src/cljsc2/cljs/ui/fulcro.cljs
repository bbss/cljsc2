(ns cljsc2.cljs.ui.fulcro
  (:require
   [fulcro.client.primitives :as prim :refer [defsc]]
   [fulcro.ui.form-state :as fs]
   [fulcro.client.mutations :as m :refer [defmutation]]
   [fulcro.ui.bootstrap3 :as bs]))

(defn clear-jupyter-events [i]
  (when (and js/window.Jupyter i)
    (js/window.Jupyter.keyboard_manager.register_events i)))

(defn render-field [component field renderer]
  (let [form         (prim/props component)
        entity-ident (prim/get-ident component form)
        id           (str (first entity-ident) "-" (second entity-ident))
        is-dirty?    (fs/dirty? form field)
        clean?       (not is-dirty?)
        validity     (fs/get-spec-validity form field)
        is-invalid?  (= :invalid validity)
        value        (get form field "")]
    (renderer {:dirty?   is-dirty?
               :ident    entity-ident
               :id       id
               :clean?   clean?
               :validity validity
               :invalid? is-invalid?
               :value    value})))

(defn input-with-label
  "A non-library helper function, written by you to help lay out your form."
  ([component field field-label validation-string input-element options]
   (render-field component field
                 (fn [{:keys [invalid? id dirty?]}]
                   (when js/window.Jupyter
                     (js/window.Jupyter.keyboard_manager.register_events input-element))
                   (bs/labeled-input (merge {:error           (when invalid? validation-string)
                                             :id              id
                                             :warning         (when dirty? "(unsaved)")
                                             :input-generator input-element}
                                            options) field-label))))
  ([component field field-label validation-string options]
   (render-field component field
                 (fn [{:keys [invalid? id dirty? value invalid ident] :as arg}]
                   (bs/labeled-input
                    (merge
                     {:value    value
                      :ref      clear-jupyter-events
                      :id       id
                      :error    (when invalid? validation-string)
                      :warning  (when dirty? "(unsaved)")
                      :onBlur   #(prim/transact!
                                  component
                                  `[(fs/mark-complete! {:entity-ident ~ident
                                                        :field        ~field})])
                      :onChange (case (:type options)
                                  "number" (fn [e]
                                             (let [value (.-value (.-target e))]
                                               (if (and (string? value)
                                                        (empty? value))
                                                 (m/set-integer! component field :value 1)
                                                 (m/set-integer! component field :value value)))
                                             )
                                  "checkbox" (fn [e]
                                               (let [value (.-checked (.-target e))]
                                                 (m/set-value! component field value)))
                                  (fn [e]
                                    (m/set-string! component field :event e)))}
                     options) field-label)))))
