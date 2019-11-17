
(ns phlox.app.container
  (:require [phlox.core :refer [defcomp rect circle text container]]
            [phlox.util :refer [hslx]]))

(defcomp
 comp-container
 (store)
 (container
  {:options {:x 100, :y 0}}
  (circle
   {:options {:x (+ 80 (:x store)), :y 100, :radius 40},
    :line-style {:width 2, :color (hslx 0 80 50), :alpha 1},
    :fill (hslx 160 80 70),
    :on {:mousedown (fn [event dispatch!] (dispatch! :add-x "a"))}})
  (rect
   {:options {:x 200, :y (+ 50 (:x store)), :width 50, :height 50},
    :line-style {:width 2, :color (hslx 200 80 80), :alpha 1},
    :fill (hslx 200 80 80),
    :on {:mousedown (fn [e dispatch!] (dispatch! :add-x "b"))}})
  (text
   {:text (str "Text demo:" (:x store)),
    :style {:font-family "Menlo",
            :font-size 16,
            :fill (hslx 200 80 (* 100 (js/Math.random))),
            :align "center"}})
  (comment container {})
  (comment graphics {})
  (comment graphics {})))
