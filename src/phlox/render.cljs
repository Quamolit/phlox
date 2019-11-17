
(ns phlox.render
  (:require ["pixi.js" :as PIXI]
            [phlox.util
             :refer
             [use-number component? element? remove-nil-values index-items map-to-object]]
            [phlox.util.lcs :refer [find-minimal-ops lcs-state-0]]))

(declare render-element)

(declare render-rect)

(declare render-container)

(declare render-circle)

(declare update-element)

(declare update-children)

(defn render-text [element]
  (let [style (:style (:props element))
        text-style (new (.-TextStyle PIXI) (map-to-object style))
        target (new (.-Text PIXI) (:text (:props element)) text-style)
        props (:props element)]
    (when (some? (:position props))
      (set! (-> target .-position .-x) (-> props :position :x))
      (set! (-> target .-position .-y) (-> props :position :y)))
    (when (some? (:pivot props))
      (set! (-> target .-pivot .-x) (-> props :pivot :x))
      (set! (-> target .-pivot .-y) (-> props :pivot :y)))
    (when (some? (:rotation props)) (set! (.-rotation target) (:rotation props)))
    target))

(defn render-rect [element dispatch!]
  (let [target (new (.-Graphics PIXI))
        props (:props element)
        line-style (:line-style props)
        options (:options props)
        events (:on props)]
    (if (some? (:fill props)) (.beginFill target (:fill props)))
    (when (some? line-style)
      (.lineStyle
       target
       (use-number (:width line-style))
       (use-number (:color line-style))
       (:alpha line-style)))
    (if (map? options)
      (.drawRect
       target
       (use-number (:x options))
       (use-number (:y options))
       (use-number (:width options))
       (use-number (:height options)))
      (js/console.warn "Unknown options" options))
    (if (some? (:fill props)) (.endFill target))
    (when (some? (:position props))
      (set! (-> target .-position .-x) (-> props :position :x))
      (set! (-> target .-position .-y) (-> props :position :y)))
    (when (some? (:pivot props))
      (set! (-> target .-pivot .-x) (-> props :pivot :x))
      (set! (-> target .-pivot .-y) (-> props :pivot :y)))
    (when (some? (:rotation props)) (set! (.-rotation target) (:rotation props)))
    (when (some? events)
      (set! (.-interactive target) true)
      (set! (.-buttonMode target) true)
      (doseq [[k listener] events]
        (.on target (name k) (fn [event] (listener event dispatch!)))))
    (doseq [child-pair (:children element)]
      (if (some? child-pair)
        (.addChild target (render-element (last child-pair) dispatch!))
        (js/console.log "nil child:" child-pair)))
    target))

(defn render-element [element dispatch!]
  (js/console.log "render-element" element)
  (case (:phlox-node element)
    :element
      (case (:name element)
        nil nil
        :container (render-container element dispatch!)
        :graphics (let [g (new (.-Graphics PIXI))] g)
        :circle (render-circle element dispatch!)
        :rect (render-rect element dispatch!)
        :text (render-text element)
        (do (println "unknown tag:" (:tag element)) {}))
    :component
      (let [renderer (:render element), tree (apply renderer (:args element))]
        (render-element tree dispatch!))
    (do (js/console.error "Unknown element:" element))))

(defn render-container [element dispatch!]
  (let [target (new (.-Container PIXI)), props (:props element), options (:options props)]
    (doseq [child-pair (:children element)]
      (if (some? child-pair)
        (.addChild target (render-element (last child-pair) dispatch!))
        (js/console.log "nil child:" child-pair)))
    (when (some? options) (set! (.-x target) (:x options)) (set! (.-y target) (:y options)))
    (when (some? (:pivot props))
      (set! (-> target .-pivot .-x) (-> props :pivot :x))
      (set! (-> target .-pivot .-y) (-> props :pivot :y)))
    (when (some? (:rotation props)) (set! (.-rotation target) (:rotation props)))
    target))

(defn render-circle [element dispatch!]
  (let [target (new (.-Graphics PIXI))
        props (:props element)
        line-style (:line-style props)
        options (:options props)
        events (:on props)]
    (when (some? (:fill props)) (.beginFill target (:fill props)))
    (when (some? line-style)
      (.lineStyle
       target
       (use-number (:width line-style))
       (use-number (:color line-style))
       (:alpha line-style)))
    (if (map? options)
      (.drawCircle
       target
       (use-number (:x options))
       (use-number (:y options))
       (use-number (:radius options)))
      (js/console.warn "Unknown options" options))
    (when (some? (:fill props)) (.endFill target))
    (when (some? events)
      (set! (.-interactive target) true)
      (set! (.-buttonMode target) true)
      (doseq [[k listener] events]
        (.on target (name k) (fn [event] (listener event dispatch!)))))
    (doseq [child-pair (:children element)]
      (if (some? child-pair)
        (.addChild target (render-element (last child-pair) dispatch!))
        (js/console.log "nil child:" child-pair)))
    target))

(defn update-circle [element old-element target dispath!]
  (let [props (:props element)
        props' (:props old-element)
        options (:options props)
        options' (:options props')
        line-style (:line-style props)
        line-style' (:line-style props')]
    (when (or (not= options options')
              (not= line-style line-style')
              (not= (:fill props) (:fill props')))
      (.clear target)
      (when (some? (:fill props)) (.beginFill target (:fill props)))
      (when (some? line-style)
        (.lineStyle
         target
         (use-number (:width line-style))
         (use-number (:color line-style))
         (:alpha line-style)))
      (if (map? options)
        (.drawCircle
         target
         (use-number (:x options))
         (use-number (:y options))
         (use-number (:radius options)))
        (js/console.warn "Unknown options" options))
      (when (some? (:fill props)) (.endFill target)))))

(defn update-container [element old-element target]
  (let [props (:props element)
        props' (:props old-element)
        options (:options props)
        options' (:options props')]
    (when (not= options options')
      (set! (.-x target) (:x options))
      (set! (.-y target) (:y options)))
    (when (not= (:pivot props) (:pivot props'))
      (set! (-> target .-pivot .-x) (-> props :pivot :x))
      (set! (-> target .-pivot .-y) (-> props :pivot :y)))
    (when (not= (:rotation props) (:rotation props'))
      (set! (.-rotation target) (:rotation props)))))

(defn update-rect [element old-element target]
  (let [props (:props element)
        props' (:props old-element)
        options (:options props)
        options' (:options props')
        line-style (:line-style props)
        line-style' (:line-style props')]
    (when (or (not= options options')
              (not= line-style line-style')
              (not= (:fill props) (:fill props')))
      (.clear target)
      (if (some? (:fill props)) (.beginFill target (:fill props)))
      (when (some? line-style)
        (.lineStyle
         target
         (use-number (:width line-style))
         (use-number (:color line-style))
         (:alpha line-style)))
      (if (map? options)
        (.drawRect
         target
         (use-number (:x options))
         (use-number (:y options))
         (use-number (:width options))
         (use-number (:height options)))
        (js/console.warn "Unknown options" options))
      (if (some? (:fill props)) (.endFill target)))
    (when (not= (:position props) (:position props'))
      (set! (-> target .-position .-x) (-> props :position :x))
      (set! (-> target .-position .-y) (-> props :position :y)))
    (when (not= (:rotation props) (:rotation props'))
      (set! (.-rotation target) (:rotation props)))
    (when (not= (:pivot props) (:pivot props'))
      (set! (-> target .-pivot .-x) (-> props :pivot :x))
      (set! (-> target .-pivot .-y) (-> props :pivot :y)))))

(defn update-text [element old-element target]
  (let [props (:props element)
        props' (:props old-element)
        text-style (:style props)
        text-style' (:style props')]
    (when (not= (:text props) (:text props')) (set! (.-text target) (:text props)))
    (when (not= text-style text-style')
      (let [new-style (new (.-TextStyle PIXI) (map-to-object text-style))]
        (set! (.-style target) new-style)))
    (when (not= (:position props) (:position props'))
      (set! (-> target .-position .-x) (-> props :position :x))
      (set! (-> target .-position .-y) (-> props :position :y)))
    (when (not= (:rotation props) (:rotation props'))
      (set! (.-rotation target) (:rotation props)))
    (when (not= (:pivot props) (:pivot props'))
      (set! (-> target .-pivot .-x) (-> props :pivot :x))
      (set! (-> target .-pivot .-y) (-> props :pivot :y)))))

(defn update-element [element old-element parent-element idx dispath! options]
  (cond
    (or (nil? element) (nil? element)) (js/console.error "Not supposed to be empty")
    (and (component? element)
         (component? old-element)
         (= (:name element) (:name old-element)))
      (if (and (= (:args element) (:args old-element)) (not (:swap? options)))
        (do (println "Same, no changes") (js/console.log (:args element) (:args old-element)))
        (recur
         (apply (:render element) (:args element))
         (apply (:render old-element) (:args old-element))
         parent-element
         idx
         dispath!
         options))
    (and (element? element) (element? old-element) (= (:name element) (:name old-element)))
      (do
       (let [target (.getChildAt parent-element idx)]
         (case (:name element)
           :container (update-container element old-element target)
           :circle (update-circle element old-element target dispath!)
           :rect (update-rect element old-element target)
           :text (update-text element old-element target)
           (do (println "not implement yet for updating:" (:name element)))))
       (update-children
        (:children element)
        (:children old-element)
        (.getChildAt parent-element idx)
        dispath!
        options))
    :else (js/console.log "replace element")))

(defn update-children [children-dict old-children-dict parent-container dispatch! options]
  (assert
   (and (every? some? (map last children-dict)) (every? some? (map last old-children-dict)))
   "children should not contain nil element")
  (let [list-ops (:acc
                  (find-minimal-ops
                   lcs-state-0
                   (map first old-children-dict)
                   (map first children-dict)))]
    (loop [idx 0, ops list-ops, xs children-dict, ys old-children-dict]
      (when-not (empty? ops)
        (let [op (first ops)]
          (case (first op)
            :remains
              (do
               (assert (= (last op) (first (first xs)) (first (first ys))) "check key")
               (update-element
                (last (first xs))
                (last (first ys))
                parent-container
                idx
                dispatch!
                options)
               (recur (inc idx) (rest ops) (rest xs) (rest ys)))
            :add
              (do
               (assert (= (last op) (first (first xs))) "check key")
               (.addChildAt
                parent-container
                (render-element (last (first xs)) dispatch!)
                idx)
               (recur (inc idx) (rest ops) (rest xs) ys))
            :remove
              (do
               (assert (= (last op) (first (first ys))) "check key")
               (.removeChildAt parent-container idx)
               (recur idx (rest ops) xs (rest ys)))
            (do (println "Unknown op:" op))))))))
