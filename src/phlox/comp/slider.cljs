
(ns phlox.comp.slider
  (:require [phlox.core
             :refer
             [defcomp g hslx rect circle text container graphics create-list]]
            [phlox.check :refer [lilac-event-map dev-check]]
            [lilac.core
             :refer
             [record+
              number+
              string+
              optional+
              tuple+
              enum+
              map+
              fn+
              any+
              keyword+
              boolean+
              vector+
              or+
              is+]]))

(def lilac-cursor (vector+ (any+ {:some? true})))

(def lilac-slider
  (record+
   {:value (number+),
    :on-change (fn+),
    :unit (optional+ (number+)),
    :fill (optional+ (number+)),
    :color (optional+ (number+)),
    :title (optional+ (string+)),
    :round? (optional+ (boolean+)),
    :max (optional+ (number+)),
    :min (optional+ (number+)),
    :position (optional+ (tuple+ [(number+) (number+)]))}
   {:check-keys? true}))

(defcomp
 comp-slider
 (states props)
 (dev-check (:cursor states) lilac-cursor)
 (dev-check props lilac-slider)
 (let [value (or (:value props) 1)
       cursor (:cursor states)
       state (or (:data states) {:v0 value, :x0 0, :dragging? false})
       title (:title props)
       unit (or (:unit props) 0.1)
       fill (or (:fill props) (hslx 0 0 30))
       color (or (:color props) (hslx 0 0 100))
       on-change (:on-change props)
       round? (:round? props)]
   (container
    {:position (:position props)}
    (rect
     {:size [120 24],
      :fill fill,
      :on {:pointerdown (fn [e d!]
             (let [x1 (-> e .-data .-global .-x)]
               (d! cursor {:dragging? true, :v0 value, :x0 x1}))),
           :pointermove (fn [e d!]
             (when (:dragging? state)
               (let [x2 (-> e .-data .-global .-x)]
                 (if (fn? on-change)
                   (on-change
                    (-> (+ (:v0 state) (* unit (- x2 (:x0 state))))
                        ((fn [v] (if round? (js/Math.round v) v)))
                        ((fn [v] (if (some? (:max props)) (min (:max props) v) v)))
                        ((fn [v] (if (some? (:min props)) (max (:min props) v) v))))
                    d!)
                   (js/console.log "[slider] missing :on-change listener"))))),
           :pointerup (fn [e d!] (d! cursor {:v0 value, :x0 0, :dragging? false})),
           :pointerupoutside (fn [e d!] (d! cursor {:v0 value, :x0 0, :dragging? false}))}}
     (text
      {:text (str "◀ " (if (number? value) (.toFixed value (if round? 0 4)) "nil") " ▶"),
       :position [4 4],
       :style {:fill color, :font-size 12, :font-family "Menlo, monospace"}})
     (text
      {:text (str (if (string? title) (str title " ") "") "◈ " unit),
       :position [0 -18],
       :style {:fill (hslx 0 0 80), :font-size 13, :font-family "Arial, sans-serif"}})))))

(def lilac-slider-point
  (record+
   {:value (number+),
    :on-change (fn+),
    :unit (optional+ (number+)),
    :fill (optional+ (number+)),
    :color (optional+ (number+)),
    :round? (optional+ (boolean+)),
    :max (optional+ (number+)),
    :min (optional+ (number+)),
    :position (optional+ (tuple+ [(number+) (number+)]))}
   {:check-keys? true}))

(defcomp
 comp-slider-point
 (states props)
 (dev-check (:cursor states) lilac-cursor)
 (dev-check props lilac-slider-point)
 (let [value (or (:value props) 1)
       cursor (:cursor states)
       state (or (:data states) {:v0 value, :x0 0, :dragging? false})
       unit (or (:unit props) 0.1)
       fill (or (:fill props) (hslx 0 0 30))
       color (or (:color props) (hslx 0 0 100))
       on-change (:on-change props)
       round? (:round? props)]
   (container
    {:position (:position props)}
    (rect
     {:size [12 12],
      :fill fill,
      :radius 4,
      :on {:pointerdown (fn [e d!]
             (let [x1 (-> e .-data .-global .-x)]
               (d! cursor {:dragging? true, :v0 value, :x0 x1}))),
           :pointermove (fn [e d!]
             (when (:dragging? state)
               (let [x2 (-> e .-data .-global .-x)]
                 (if (fn? on-change)
                   (on-change
                    (-> (+ (:v0 state) (* unit (- x2 (:x0 state))))
                        ((fn [v] (if round? (js/Math.round v) v)))
                        ((fn [v] (if (some? (:max props)) (min (:max props) v) v)))
                        ((fn [v] (if (some? (:min props)) (max (:min props) v) v))))
                    d!)
                   (js/console.log "[slider] missing :on-change listener"))))),
           :pointerup (fn [e d!] (d! cursor {:v0 value, :x0 0, :dragging? false})),
           :pointerupoutside (fn [e d!] (d! cursor {:v0 value, :x0 0, :dragging? false}))}}
     (text
      {:text (str (if (number? value) (.toFixed value (if round? 0 4)) "nil")),
       :position [16 1],
       :style {:fill color, :font-size 10, :font-family "Menlo, monospace"}})))))
