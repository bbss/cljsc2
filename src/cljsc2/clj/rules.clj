(ns cljsc2.clj.rules)

(def can-build-rule
  '[[(can-build ?name ?build-ability ?builder)
     [?build-me :unit-type/name ?name]
     [?build-me :unit-type/ability-id ?build-ability]
     [(+ 880000 ?build-ability) ?ability-e-id]
     [?builder-type-id :unit-type/abilities ?ability-e-id]
     [?builder-type-id :unit-type/unit-id ?unit-id]
     [?builder :unit/unit-type ?unit-id]]])

(def currently-doing-rule
  '[[(currently-doing ?unit-id ?ability-id)
     [?unit-id :unit/orders ?order-id]
     [?order-id :order/ability-id ?ability-id]]])

(def units-of-type-rule
  '[[(units-of-type ?type-name ?unit-tag)
     [?unit-type-e-id :unit-type/name ?type-name]
     [?unit-type-e-id :unit-type/unit-id ?type]
     [?unit-tag :unit/unit-type ?type]]])

(def has-order-or-nil-order-rule
  '[[(has-order-or-nil-order ?unit-tag ?order)
     [?unit-tag :unit/orders ?order]]
    [(has-order-or-nil-order ?unit-tag)
     [(= ?order nil)]]])
