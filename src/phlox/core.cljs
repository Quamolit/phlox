
(ns phlox.core
  (:require ["pixi.js" :as PIXI]
            [phlox.render :refer [render-element update-element update-children]]
            [phlox.util :refer [index-items remove-nil-values detect-func-in-map?]]
            ["./hue-to-rgb" :refer [hslToRgb]]
            [phlox.check
             :refer
             [dev-check
              lilac-color
              lilac-rect
              lilac-text
              lilac-container
              lilac-graphics
              lilac-point
              lilac-circle
              dev-check-message
              lilac-line-style]]
            [lilac.core
             :refer
             [record+
              number+
              string+
              optional+
              tuple+
              map+
              fn+
              keyword+
              boolean+
              vector+
              or+]]
            [phlox.keyboard :refer [handle-keyboard-events]]
            [memof.core :as memof])
  (:require-macros [phlox.core]))

(defonce *app (atom nil))

(defonce *events-element (atom nil))

(defonce *phlox-caches (atom (memof/new-states {:trigger-loop 200, :elapse-loop 600})))

(defonce *renderer (atom nil))

(defonce *tree-element (atom nil))

(defn >> [states k]
  (let [parent-cursor (or (:cursor states) []), branch (get states k)]
    (assoc branch :cursor (conj parent-cursor k))))

(defn call-comp-helper [f params]
  (if (or (some fn? params))
    (apply f params)
    (let [v (memof/access-record *phlox-caches f params)]
      (if (some? v)
        v
        (let [result (apply f params)]
          (memof/write-record! *phlox-caches f params result)
          result)))))

(defn create-element [tag props children]
  {:name tag,
   :phlox-node :element,
   :props props,
   :children (remove-nil-values (index-items children))})

(defn circle [props & children]
  (dev-check props lilac-circle)
  (create-element :circle props children))

(defn clear-phlox-caches! [] (memof/reset-entries! *phlox-caches))

(defn container [props & children]
  (dev-check props lilac-container)
  (create-element :container props children))

(defn create-list
  ([props children] (create-list :container props children))
  ([tag props children]
   {:name tag, :phlox-node :element, :props props, :children (remove-nil-values children)}))

(def lilac-arc
  (record+
   {:center lilac-point,
    :angle (optional+ (tuple+ [(number+) (number+)])),
    :radian (optional+ (tuple+ [(number+) (number+)])),
    :radius (number+),
    :anticlockwise? (optional+ (boolean+))}
   {:check-keys? true}))

(def lilac-arc-to
  (record+ {:p1 lilac-point, :p2 lilac-point, :radius (number+)} {:exact-keys? true}))

(def lilac-begin-fill
  (record+ {:color lilac-color, :alpha (optional+ (number+))} {:check-keys? true}))

(def lilac-bezier-to
  (record+ {:p1 lilac-point, :p2 lilac-point, :to-p lilac-point} {:exact-keys? true}))

(def lilac-quodratic-to (record+ {:p1 lilac-point, :to-p lilac-point} {:exact-keys? true}))

(defn g [op data]
  (case op
    :move-to (dev-check-message "check :move-to" data lilac-point)
    :line-to (dev-check-message "check :line-to" data lilac-point)
    :line-style (dev-check-message "check :line-style" data lilac-line-style)
    :begin-fill (dev-check-message "check :fill" data lilac-begin-fill)
    :end-fill (do)
    :close-path (do)
    :arc (dev-check-message "check :arc" data lilac-arc)
    :arc-to (dev-check-message "check :arc-to" data lilac-arc-to)
    :bezier-to (dev-check-message "check :bezier-to" data lilac-bezier-to)
    :quadratic-to (dev-check-message "check :quadratic-to" data lilac-quodratic-to)
    :begin-hole (do)
    :end-hole (do)
    (js/console.warn "not supported:" op))
  [op data])

(defn graphics [props & children]
  (dev-check props lilac-graphics)
  (create-element :graphics props children))

(defn hslx [h s l]
  (let [[r g b] (hslToRgb (/ h 360) (* 0.01 s) (* 0.01 l))
        r0 (PIXI/utils.rgb2hex (array r g b))]
    r0))

(defn mount-app! [app dispatch!]
  (let [element-tree (render-element app dispatch!)] (.addChild (.-stage @*app) element-tree)))

(defn rect [props & children]
  (dev-check props lilac-rect)
  (create-element :rect props children))

(defn rerender-app! [app dispatch! options]
  (comment js/console.log "rerender tree" app @*tree-element)
  (update-children
   (list [0 app])
   (list [0 @*tree-element])
   (.-stage @*app)
   dispatch!
   options))

(defn render! [expanded-app dispatch! options]
  (when (nil? @*app)
    (let [pixi-app (PIXI/Application.
                    (clj->js
                     {:backgroundColor (hslx 0 0 0),
                      :antialias true,
                      :autoDensity true,
                      :resolution 2,
                      :width js/window.innerWidth,
                      :height js/window.innerHeight,
                      :interactive true}))]
      (reset! *app pixi-app)
      (-> js/document .-body (.appendChild (.-view pixi-app)))
      (.addEventListener
       js/window
       "resize"
       (fn [] (-> pixi-app .-renderer (.resize js/window.innerWidth js/window.innerHeight)))))
    (set! js/window._phloxTree @*app))
  (let [wrap-dispatch (fn [op data]
                        (if (vector? op) (dispatch! :states [op data]) (dispatch! op data)))]
    (comment js/console.log "render!" expanded-app)
    (if (nil? @*tree-element)
      (do
       (mount-app! expanded-app wrap-dispatch)
       (handle-keyboard-events *tree-element wrap-dispatch))
      (rerender-app! expanded-app wrap-dispatch options))
    (reset! *tree-element expanded-app))
  (memof/new-loop! *phlox-caches))

(defn text [props & children]
  (dev-check props lilac-text)
  (create-element :text props children))
