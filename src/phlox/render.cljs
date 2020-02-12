
(ns phlox.render
  (:require ["pixi.js" :as PIXI]
            [phlox.util
             :refer
             [use-number component? element? remove-nil-values index-items map-to-object]]
            [phlox.util.lcs :refer [find-minimal-ops lcs-state-0]]
            [phlox.render.draw
             :refer
             [call-graphics-ops
              set-position
              set-pivot
              set-rotation
              set-alpha
              add-events
              update-events
              set-line-style
              draw-circle
              draw-rect
              lilac-color]]
            [phlox.check :refer [dev-check]]
            [lilac.core
             :refer
             [record+ number+ string+ optional+ tuple+ map+ fn+ keyword+ vector+]]))

(declare render-children)

(declare render-element)

(declare render-rect)

(declare render-container)

(declare render-graphics)

(declare render-circle)

(declare render-text)

(declare update-element)

(declare update-children)

(def in-dev? (do ^boolean js/goog.DEBUG))

(def lilac-line-style (record+ {:width (number+), :color (number+), :alpha (number+)}))

(def lilac-point (tuple+ [(number+) (number+)]))

(def lilac-circle
  (record+
   {:line-style (optional+ lilac-line-style),
    :on (optional+ (map+ (keyword+) (fn+))),
    :position lilac-point,
    :radius (number+),
    :fill (number+),
    :alpha (optional+ (number+)),
    :rotation (optional+ (number+))}
   {:check-keys? true}))

(def lilac-container
  (record+
   {:position (optional+ lilac-point),
    :rotation (optional+ (number+)),
    :pivot (optional+ lilac-point),
    :alpha (optional+ (number+))}
   {:check-keys? true}))

(def lilac-graphics
  (record+
   {:on (optional+ (map+ (keyword+) (fn+))),
    :position (optional+ lilac-point),
    :pivot (optional+ lilac-point),
    :alpha (optional+ (number+)),
    :rotation (optional+ (number+)),
    :ops (vector+ (tuple+ [(keyword+)]))}
   {:check-keys? true}))

(def lilac-rect
  (record+
   {:line-style (optional+ lilac-line-style),
    :on (optional+ (map+ (keyword+) (fn+))),
    :position (optional+ lilac-point),
    :size (optional+ lilac-point),
    :pivot (optional+ lilac-point),
    :alpha (optional+ (number+)),
    :rotation (optional+ (number+)),
    :fill (optional+ lilac-color)}
   {:check-keys? true}))

(def lilac-text
  (record+
   {:text (string+),
    :style (record+
            {:fill (optional+ lilac-color),
             :font-size (optional+ (number+)),
             :font-family (optional+ (string+)),
             :align (optional+ (string+)),
             :font-weight (optional+ (number+))}
            {:check-keys? true}),
    :position (optional+ lilac-point),
    :pivot (optional+ (number+)),
    :rotation (optional+ (number+)),
    :alpha (optional+ (number+))}
   {:check-keys? true}))

(defn render-text [element dispatch!]
  (let [style (:style (:props element))
        text-style (new (.-TextStyle PIXI) (map-to-object style))
        target (new (.-Text PIXI) (:text (:props element)) text-style)
        props (:props element)]
    (dev-check props lilac-text)
    (set-position target (:position props))
    (set-pivot target (:pivot props))
    (set-rotation target (:rotation props))
    (set-alpha target (:alpha props))
    (render-children target (:children element) dispatch!)
    target))

(defn render-rect [element dispatch!]
  (let [target (new (.-Graphics PIXI))
        props (:props element)
        line-style (:line-style props)
        events (:on props)]
    (dev-check props lilac-rect)
    (if (some? (:fill props)) (.beginFill target (:fill props)))
    (set-line-style target line-style)
    (draw-rect target (:position props) (:size props))
    (if (some? (:fill props)) (.endFill target))
    (set-pivot target (:pivot props))
    (set-rotation target (:rotation props))
    (set-alpha target (:alpha props))
    (add-events target events dispatch!)
    (render-children target (:children element) dispatch!)
    target))

(defn render-graphics [element dispatch!]
  (let [target (new (.-Graphics PIXI))
        props (:props element)
        ops (:ops props)
        events (:on props)]
    (dev-check props lilac-graphics)
    (call-graphics-ops target ops)
    (set-rotation target (:rotation props))
    (set-pivot target (:pivot props))
    (set-position target (:position props))
    (set-alpha target (:alpha props))
    (add-events target events dispatch!)
    (render-children target (:children element) dispatch!)
    target))

(defn render-element [element dispatch!]
  (case (:phlox-node element)
    :element
      (case (:name element)
        nil nil
        :container (render-container element dispatch!)
        :graphics (render-graphics element dispatch!)
        :circle (render-circle element dispatch!)
        :rect (render-rect element dispatch!)
        :text (render-text element dispatch!)
        (do (println "unknown tag:" (:tag element)) {}))
    :component (render-element (:tree element) dispatch!)
    (do (js/console.error "Unknown element:" element))))

(defn render-container [element dispatch!]
  (let [target (new (.-Container PIXI)), props (:props element)]
    (dev-check props lilac-container)
    (render-children target (:children element) dispatch!)
    (set-position target (:position props))
    (set-rotation target (:rotation props))
    (set-pivot target (:pivot props))
    (set-alpha target (:alpha props))
    target))

(defn render-circle [element dispatch!]
  (let [target (new (.-Graphics PIXI))
        props (:props element)
        line-style (:line-style props)
        position (:position props)
        events (:on props)]
    (dev-check props lilac-circle)
    (when (some? (:fill props)) (.beginFill target (:fill props)))
    (set-line-style target line-style)
    (draw-circle target position (:radius props))
    (when (some? (:fill props)) (.endFill target))
    (add-events target events dispatch!)
    (set-alpha target (:alpha props))
    (render-children target (:children element) dispatch!)
    target))

(defn render-children [target children dispatch!]
  (doseq [child-pair children]
    (if (some? child-pair)
      (.addChild target (render-element (peek child-pair) dispatch!))
      (js/console.log "nil child:" child-pair))))

(defn update-circle [element old-element target dispatch!]
  (let [props (:props element)
        props' (:props old-element)
        position (:position props)
        position' (:position props')
        radius (:radius props)
        radius' (:radius props')
        line-style (:line-style props)
        line-style' (:line-style props')]
    (dev-check props lilac-circle)
    (when (or (not= position position')
              (not= radius radius')
              (not= line-style line-style')
              (not= (:fill props) (:fill props')))
      (.clear target)
      (when (some? (:fill props)) (.beginFill target (:fill props)))
      (set-line-style target line-style)
      (draw-circle target position (:radius props))
      (when (some? (:fill props)) (.endFill target))
      (when (not= (:alpha props) (:alpha props')) (set-alpha target (:alpha props))))
    (update-events target (-> element :props :on) (-> old-element :props :on) dispatch!)))

(defn update-container [element old-element target]
  (let [props (:props element), props' (:props old-element)]
    (dev-check props lilac-container)
    (when (not= (:position props) (:position props'))
      (set-position target (:position props)))
    (when (not= (:pivot props) (:pivot props')) (set-pivot target (:pivot props)))
    (when (not= (:rotation props) (:rotation props'))
      (set-rotation target (:rotation props)))
    (when (not= (:alpha props) (:alpha props')) (set-alpha target (:alpha props)))))

(defn update-graphics [element old-element target dispatch!]
  (let [props (:props element)
        props' (:props old-element)
        ops (:ops props)
        ops' (:ops props')]
    (dev-check props lilac-graphics)
    (when (not= ops ops') (.clear target) (call-graphics-ops target ops))
    (when (not= (:position props) (:position props'))
      (set-position target (:position props)))
    (when (not= (:rotation props) (:rotation props'))
      (set-rotation target (:rotation props)))
    (when (not= (:pivot props) (:pivot props')) (set-pivot target (:pivot props)))
    (when (not= (:alpha props) (:alpha props')) (set-alpha target (:alpha props)))
    (update-events target (-> element :props :on) (-> old-element :props :on) dispatch!)))

(defn update-rect [element old-element target dispatch!]
  (let [props (:props element)
        props' (:props old-element)
        position (:position props)
        position' (:position props')
        size (:size props)
        size' (:size props')
        line-style (:line-style props)
        line-style' (:line-style props')]
    (dev-check props lilac-rect)
    (when (or (not= position position')
              (not= size size')
              (not= line-style line-style')
              (not= (:fill props) (:fill props')))
      (.clear target)
      (if (some? (:fill props)) (.beginFill target (:fill props)))
      (set-line-style target line-style)
      (draw-rect target position size)
      (if (some? (:fill props)) (.endFill target)))
    (when (not= (:rotation props) (:rotation props'))
      (set-rotation target (:rotation props)))
    (when (not= (:pivot props) (:pivot props')) (set-pivot target (:pivot props)))
    (when (not= (:alpha props) (:alpha props')) (set-alpha target (:alpha props)))
    (update-events target (-> element :props :on) (-> old-element :props :on) dispatch!)))

(defn update-text [element old-element target]
  (let [props (:props element)
        props' (:props old-element)
        text-style (:style props)
        text-style' (:style props')]
    (dev-check props lilac-text)
    (when (not= (:text props) (:text props')) (set! (.-text target) (:text props)))
    (when (not= text-style text-style')
      (let [new-style (new (.-TextStyle PIXI) (map-to-object text-style))]
        (set! (.-style target) new-style)))
    (when (not= (:position props) (:position props'))
      (set-position target (:position props)))
    (when (not= (:rotation props) (:rotation props'))
      (set-rotation target (:rotation props)))
    (when (not= (:pivot props) (:pivot props')) (set-pivot target (:pivot props)))
    (when (not= (:alpha props) (:alpha props')) (set-alpha target (:alpha props)))))

(defn update-element [element old-element parent-element idx dispatch! options]
  (cond
    (or (nil? element) (nil? element)) (js/console.error "Not supposed to be empty")
    (and (component? element)
         (component? old-element)
         (= (:name element) (:name old-element)))
      (if (and (= (:args element) (:args old-element)) (not (:swap? options)))
        (comment
         do
         (js/console.log "Same, no changes" (:name element))
         (js/console.log (:args element) (:args old-element)))
        (recur (:tree element) (:tree old-element) parent-element idx dispatch! options))
    (and (component? element) (element? old-element))
      (recur (:tree element) old-element parent-element idx dispatch! options)
    (and (element? element) (component? old-element))
      (recur element (:tree old-element) parent-element idx dispatch! options)
    (and (element? element) (element? old-element) (= (:name element) (:name old-element)))
      (do
       (let [target (.getChildAt parent-element idx)]
         (case (:name element)
           :container (update-container element old-element target)
           :circle (update-circle element old-element target dispatch!)
           :rect (update-rect element old-element target dispatch!)
           :text (update-text element old-element target)
           :graphics (update-graphics element old-element target dispatch!)
           (do (println "not implement yet for updating:" (:name element)))))
       (update-children
        (:children element)
        (:children old-element)
        (.getChildAt parent-element idx)
        dispatch!
        options))
    (not= (:name element) (:name old-element))
      (do
       (.removeChildAt parent-element idx)
       (.addChildAt parent-element (render-element element dispatch!) idx))
    :else (js/console.warn "Unknown case:" element old-element)))

(defn update-children [children-dict old-children-dict parent-container dispatch! options]
  (when in-dev?
    (assert
     (and (every? some? (map peek children-dict))
          (every? some? (map peek old-children-dict)))
     "children should not contain nil element"))
  (let [list-ops (:acc
                  (find-minimal-ops
                   lcs-state-0
                   (map first old-children-dict)
                   (map first children-dict)))]
    (comment js/console.log "ops" list-ops old-children-dict children-dict)
    (loop [idx 0, ops list-ops, xs children-dict, ys old-children-dict]
      (when-not (empty? ops)
        (let [op (first ops)]
          (case (first op)
            :remains
              (do
               (when in-dev?
                 (assert (= (peek op) (first (first xs)) (first (first ys))) "check key"))
               (update-element
                (peek (first xs))
                (peek (first ys))
                parent-container
                idx
                dispatch!
                options)
               (recur (inc idx) (rest ops) (rest xs) (rest ys)))
            :add
              (do
               (when in-dev? (assert (= (peek op) (first (first xs))) "check key"))
               (.addChildAt
                parent-container
                (render-element (peek (first xs)) dispatch!)
                idx)
               (recur (inc idx) (rest ops) (rest xs) ys))
            :remove
              (do
               (when in-dev? (assert (= (peek op) (first (first ys))) "check key"))
               (.removeChildAt parent-container idx)
               (recur idx (rest ops) xs (rest ys)))
            (do (println "Unknown op:" op))))))))
