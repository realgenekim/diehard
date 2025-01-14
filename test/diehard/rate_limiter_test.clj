(ns diehard.rate-limiter-test
  (:require  [clojure.test :as t]
             [diehard.rate-limiter :as r])
  (:import [java.util.concurrent Executors]))

(t/deftest rate-limiter-test
  (t/testing "base case"
    (let [rate 1000
          threads 32
          pool (Executors/newFixedThreadPool threads)
          rl (r/rate-limiter {:rate rate})
          counter (atom 0)
          time-secs 2]
      (doseq [_ (range threads)]
        (.submit pool (cast Runnable (fn []
                                       (while true
                                         (r/acquire! rl)
                                         (swap! counter inc))))))
      (Thread/sleep (* time-secs 1000))
      (.shutdown pool)
      (t/is (< (Math/abs (- @counter (* rate time-secs)))
               ;; error tolerance 3%
               (* 0.03 (* rate time-secs))))))

  (t/testing "rate limits less than 1.0"
    (let [rate 0.5
          threads 1
          pool (Executors/newFixedThreadPool threads)
          rl (r/rate-limiter {:rate rate})
          counter (atom 0)
          time-secs 5]
      (doseq [_ (range threads)]
        (.submit pool (cast Runnable (fn []
                                       (while true
                                         (r/acquire! rl)
                                         (swap! counter inc))))))
      (Thread/sleep (* time-secs 1000))
      (.shutdown pool)
      (let [variance (Math/abs (- @counter (* rate time-secs)))]
        (t/is (<= variance
                ;; error tolerance: 0.5  -- larger, because we are dealing with slower ticks
                0.5))))))
