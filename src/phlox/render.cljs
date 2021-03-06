
(ns phlox.render
  (:require ["pixi.js" :as PIXI]
            [phlox.util
             :refer
             [use-number
              component?
              element?
              remove-nil-values
              index-items
              convert-line-style]]
            [phlox.util.lcs :refer [find-minimal-ops lcs-state-0]]
            [phlox.render.draw
             :refer
             [call-graphics-ops
              update-position
              update-pivot
              update-rotation
              update-alpha
              update-events
              draw-circle
              draw-rect
              init-events
              init-position
              init-pivot
              init-angle
              init-rotation
              init-alpha
              init-line-style]]
            [phlox.check
             :refer
             [dev-check
              lilac-color
              lilac-rect
              lilac-text
              lilac-container
              lilac-graphics
              lilac-circle]]))

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

(defn init-fill [target color]
  (.endFill target)
  (if (some? color) (.beginFill target color)))

(defn render-text [element dispatch!]
  (let [style (:style (:props element))
        text-style (new (.-TextStyle PIXI) (convert-line-style style))
        target (new (.-Text PIXI) (:text (:props element)) text-style)
        props (:props element)]
    (init-position target (:position props))
    (init-pivot target (:pivot props))
    (init-angle target (:angle props))
    (init-rotation target (:rotation props))
    (init-alpha target (:alpha props))
    (render-children target (:children element) dispatch!)
    target))

(defn render-rect [element dispatch!]
  (let [target (new (.-Graphics PIXI)), props (:props element), events (:on props)]
    (init-fill target (:fill props))
    (init-line-style target (:line-style props))
    (draw-rect target (:size props) (:radius props))
    (init-position target (:position props))
    (init-pivot target (:pivot props))
    (init-rotation target (:rotation props))
    (init-angle target (:angle props))
    (init-alpha target (:alpha props))
    (init-events target events dispatch!)
    (render-children target (:children element) dispatch!)
    target))

(defn render-graphics [element dispatch!]
  (let [target (new (.-Graphics PIXI))
        props (:props element)
        ops (:ops props)
        events (:on props)]
    (dev-check props lilac-graphics)
    (call-graphics-ops target ops)
    (init-rotation target (:rotation props))
    (init-angle target (:angle props))
    (init-pivot target (:pivot props))
    (init-position target (:position props))
    (init-alpha target (:alpha props))
    (init-events target events dispatch!)
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
    (render-children target (:children element) dispatch!)
    (init-position target (:position props))
    (init-rotation target (:rotation props))
    (init-angle target (:angle props))
    (init-pivot target (:pivot props))
    (init-alpha target (:alpha props))
    target))

(defn render-circle [element dispatch!]
  (let [target (new (.-Graphics PIXI))
        props (:props element)
        line-style (:line-style props)
        position (:position props)
        events (:on props)]
    (init-fill target (:fill props))
    (init-line-style target line-style)
    (draw-circle target (:radius props))
    (init-events target events dispatch!)
    (init-position target (:position props))
    (init-rotation target (:rotation props))
    (init-pivot target (:pivot props))
    (init-angle target (:angle props))
    (init-alpha target (:alpha props))
    (render-children target (:children element) dispatch!)
    target))

(defn render-children [target children dispatch!]
  (doseq [child-pair children]
    (if (some? child-pair)
      (.addChild target (render-element (peek child-pair) dispatch!))
      (js/console.log "nil child:" child-pair))))

(defn update-angle [target v v0] (when (not= v v0) (set! (.-angle target) v)))

(defn update-circle [element old-element target dispatch!]
  (let [props (:props element)
        props' (:props old-element)
        position (:position props)
        position' (:position props')
        radius (:radius props)
        radius' (:radius props')
        line-style (:line-style props)
        line-style' (:line-style props')]
    (when (or (not= position position')
              (not= radius radius')
              (not= line-style line-style')
              (not= (:fill props) (:fill props')))
      (.clear target)
      (init-fill target (:fill props))
      (init-line-style target line-style)
      (draw-circle target (:radius props)))
    (update-position target (:position props) (:position props'))
    (update-alpha target (:alpha props) (:alpha props'))
    (update-angle target (:angle props) (:angle props'))
    (update-rotation target (:rotation props) (:rotation props'))
    (update-pivot target (:pivot props) (:pivot props'))
    (update-events target (-> element :props :on) (-> old-element :props :on) dispatch!)))

(defn update-container [element old-element target]
  (let [props (:props element), props' (:props old-element)]
    (update-position target (:position props) (:position props'))
    (update-pivot target (:pivot props) (:pivot props'))
    (update-angle target (:angle props) (:angle props'))
    (update-rotation target (:rotation props) (:rotation props'))
    (update-alpha target (:alpha props) (:alpha props'))))

(defn update-graphics [element old-element target dispatch!]
  (let [props (:props element)
        props' (:props old-element)
        ops (:ops props)
        ops' (:ops props')]
    (when (not= ops ops') (.clear target) (call-graphics-ops target ops))
    (update-position target (:position props) (:position props'))
    (update-rotation target (:rotation props) (:rotation props'))
    (update-angle target (:angle props) (:angle props'))
    (update-pivot target (:pivot props) (:pivot props'))
    (update-alpha target (:alpha props) (:alpha props'))
    (update-events target (-> element :props :on) (-> old-element :props :on) dispatch!)))

(defn update-rect [element old-element target dispatch!]
  (let [props (:props element)
        props' (:props old-element)
        position (:position props)
        position' (:position props')
        size (:size props)
        size' (:size props')
        radius (:radius props)
        radius' (:radius props')
        line-style (:line-style props)
        line-style' (:line-style props')]
    (when (or (not= size size')
              (not= radius radius')
              (not= line-style line-style')
              (not= (:fill props) (:fill props')))
      (.clear target)
      (init-fill target (:fill props))
      (init-line-style target line-style)
      (draw-rect target size (:radius props)))
    (update-position target (:position props) (:position props'))
    (update-rotation target (:rotation props) (:rotation props'))
    (update-angle target (:angle props) (:angle props'))
    (update-pivot target (:pivot props) (:pivot props'))
    (update-alpha target (:alpha props) (:alpha props'))
    (update-events target (-> element :props :on) (-> old-element :props :on) dispatch!)))

(defn update-text [element old-element target]
  (let [props (:props element)
        props' (:props old-element)
        text-style (:style props)
        text-style' (:style props')]
    (when (not= (:text props) (:text props')) (set! (.-text target) (:text props)))
    (when (not= text-style text-style')
      (let [new-style (new (.-TextStyle PIXI) (convert-line-style text-style))]
        (set! (.-style target) new-style)))
    (update-position target (:position props) (:position props'))
    (update-rotation target (:rotation props) (:rotation props'))
    (update-angle target (:angle props) (:angle props'))
    (update-pivot target (:pivot props) (:pivot props'))
    (update-alpha target (:alpha props) (:alpha props'))))

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
  (let [list-ops (find-minimal-ops
                  lcs-state-0
                  (map first old-children-dict)
                  (map first children-dict))]
    (comment js/console.log "ops" (:total list-ops))
    (loop [idx 0, ops (:acc list-ops), xs children-dict, ys old-children-dict]
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
