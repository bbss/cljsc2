(ns cljsc2.clj.rules)

(def can-build-rule
  '[[(can-build ?name ?build-ability ?builder)
     [?build-me :unit-type/name ?name]
     [?build-me :unit-type/ability-id ?build-ability]
     [(+ 880000 ?build-ability) ?ability-e-id]
     [?builder-type-id :unit-type/abilities ?ability-e-id]
     [?builder-type-id :unit-type/unit-id ?unit-id]
     [?builder :unit/unit-type ?unit-id]]])

(def can-build-type-rule
  '[[(can-build-type ?type ?build-ability ?builder)
     [?build-me :unit-type/unit-id ?type]
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

(def clamp-at-rule
  '[[(clamp ?number ?clamp-at ?result)
     [(< ?clamp-at ?number)]
     [(+ ?clamp-at 0) ?result]]
    [(clamp ?number ?clamp-at ?result)
     [(>= ?clamp-at ?number)]
     [(+ ?number 0) ?result]]])

(def has-tech-lab-rule
  '[[(has-tech-lab ?unit-name ?unit)
    (units-of-type ?unit-name ?unit)
    [?unit :unit/x ?u-x]
    [?unit :unit/y ?u-y]
    [(+ ?u-x 2.5) ?correct-x]
    [(- ?u-y 0.5) ?correct-y]
    [?tech-lab :unit/unit-type 37]
    [?tech-lab :unit/x ?tech-x]
    [?tech-lab :unit/y ?tech-y]
    [(= ?correct-x ?tech-x)]
    [(= ?correct-y ?tech-y)]
    ]])

(def has-reactor-rule
  '[[(has-reactor ?unit-name ?unit)
     (units-of-type ?unit-name ?unit)
     [?unit :unit/x ?u-x]
     [?unit :unit/y ?u-y]
     [(+ ?u-x 2.5) ?correct-x]
     [(- ?u-y 0.5) ?correct-y]
     [?reactor :unit/unit-type 37]
     [?reactor :unit/x ?reactor-x]
     [?reactor :unit/y ?reactor-y]
     [(= ?correct-x ?reactor-x)]
     [(= ?correct-y ?reactor-y)]
     ]])
