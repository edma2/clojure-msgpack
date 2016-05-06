(ns msgpack.core)

(defprotocol Reader
  (read-uint8  [_])
  (read-uint16 [_])
  (read-uint32 [_])
  (read-int8   [_])
  (read-int16  [_])
  (read-int32  [_])
  (read-float  [_])
  (read-double [_])
  (read-bytes  [_ n])
  (rewind      [_ n]))

(defprotocol Writer
  (write-uint8  [_ x])
  (write-uint16 [_ x])
  (write-uint32 [_ x])
  (write-int8   [_ x])
  (write-int16  [_ x])
  (write-int32  [_ x])
  (write-float  [_ x])
  (write-double [_ x])
  (write-bytes  [_ x]))

(defn- offsetr!
  [^DataViewReader r ^number n x]
  (let [^number o (.-offset r)]
    (set! (.-offset r) (+ n o)) x))

(defn- offsetw!
  [^DataViewWriter w ^number n _]
  (let [^number o (.-offset w)]
    (set! (.-offset w) (+ n o)) w))

(deftype DataViewReader [^js/DataView view ^:mutable ^number offset]
  Reader
  (read-uint8  [this] (offsetr! this 1 (.getUint8   view offset)))
  (read-uint16 [this] (offsetr! this 2 (.getUint16  view offset)))
  (read-uint32 [this] (offsetr! this 4 (.getUint32  view offset)))
  (read-int8   [this] (offsetr! this 1 (.getInt8    view offset)))
  (read-int16  [this] (offsetr! this 2 (.getInt16   view offset)))
  (read-int32  [this] (offsetr! this 4 (.getInt32   view offset)))
  (read-float  [this] (offsetr! this 4 (.getFloat32 view offset)))
  (read-double [this] (offsetr! this 8 (.getFloat64 view offset)))
  (read-bytes  [this n]
    (offsetr! this n (-> (.-buffer view) (js/Uint8ClampedArray. offset n))))
  (rewind [this n]
    (set! offset (- offset n))
    this))

(deftype DataViewWriter [^js/DataView view ^:mutable ^number offset]
  Writer
  (write-uint8  [this x] (offsetw! this 1 (.setUint8   view offset x)))
  (write-uint16 [this x] (offsetw! this 2 (.setUint16  view offset x)))
  (write-uint32 [this x] (offsetw! this 4 (.setUint32  view offset x)))
  (write-int8   [this x] (offsetw! this 1 (.setInt8    view offset x)))
  (write-int16  [this x] (offsetw! this 2 (.setInt16   view offset x)))
  (write-int32  [this x] (offsetw! this 4 (.setInt32   view offset x)))
  (write-float  [this x] (offsetw! this 4 (.setFloat32 view offset x)))
  (write-double [this x] (offsetw! this 8 (.setFloat64 view offset x)))
  (write-bytes  [this x]
    (let [l (.-length x)
          v (js/Uint8ClampedArray. (.-buffer view) offset l)]
      (.set v x)
      (offsetw! this l this))))

(defn- utf8-write-str
  [^DataViewWriter w s]
  (doseq [c s]
    (let [cp (.charCodeAt c 0)]
      (condp >= cp
        0x80
        (write-uint8
         w (-> cp (bit-shift-right-zero-fill 0) (bit-and 0x7f) (bit-or 0x00)))
        0x800
        (do
          (write-uint8
           w (-> cp (bit-shift-right-zero-fill 6) (bit-and 0x1f) (bit-or 0xc0)))
          (write-uint8
           w (-> cp (bit-shift-right-zero-fill 0) (bit-and 0x3f) (bit-or 0x80))))
        0x10000
        (do
          (write-uint8
           w (-> cp (bit-shift-right-zero-fill 12) (bit-and 0x0f) (bit-or 0xe0)))
          (write-uint8
           w (-> cp (bit-shift-right-zero-fill  6) (bit-and 0x3f) (bit-or 0x80)))
          (write-uint8
           w (-> cp (bit-shift-right-zero-fill  0) (bit-and 0x3f) (bit-or 0x80))))
        0x110000
        (do
          (write-uint8
           w (-> cp (bit-shift-right-zero-fill 18) (bit-and 0x07) (bit-or 0xf0)))
          (write-uint8
           w (-> cp (bit-shift-right-zero-fill 12) (bit-and 0x3f) (bit-or 0x80)))
          (write-uint8
           w (-> cp (bit-shift-right-zero-fill  6) (bit-and 0x3f) (bit-or 0x80))))
          (write-uint8
           w (-> cp (bit-shift-right-zero-fill  0) (bit-and 0x3f) (bit-or 0x80)))))))

(defn- utf8-read-str
  [^DataViewReader r n]
  (loop [s "" i 0]
    (if (< i n)
        (let [b (read-uint8 r)]
          (cond

            ;; One byte character
            (= (bit-and 0x80 b) 0x00)
            (recur (str s (js/String.fromCharCode b)) (+ 1 i))

            ;; Two byte character
            (= (bit-and 0xe0 b) 0xc0)
            (recur (str s (js/String.fromCharCode
                           (bit-or
                            (bit-shift-left (bit-and 0x0f b) 6)
                            (bit-and (read-uint8 r) 0x3f)))) (+ 2 i))

            ;; Three byte character
            (= (bit-and 0xf0 b) 0xe0)
            (recur (str s (js/String.fromCharCode
                           (bit-or
                            (bit-shift-left (bit-and 0x0f b) 12)
                            (bit-shift-left (bit-and (read-uint8 r) 0x3f) 6)
                            (bit-shift-left (bit-and (read-uint8 r) 0x3f) 0)))) (+ 3 i))

            ;; Four byte character
            (= (bit-and 0xf8 b) 0xf0)
            (recur (str s (js/String.fromCharCode
                           (bit-or
                            (bit-shift-left (bit-and 0x07 b) 18)
                            (bit-shift-left (bit-and (read-uint8 r) 0x3f) 12)
                            (bit-shift-left (bit-and (read-uint8 r) 0x3f) 6)
                            (bit-shift-left (bit-and (read-uint8 r) 0x3f) 0)))) (+ 4 i))))
        s)))

(defn- utf8-byte-length
  [^js/String s]
  (reduce
   (fn [n c]
     (condp >= (.charCodeAt c 0)
       0x80     (+ 1 n)
       0x800    (+ 2 n)
       0x10000  (+ 3 n)
       0x110000 (+ 4 n)))
   0 s))

(defn- pack-bytes
  [x w]
  (let [l (.-length x)]
    (cond
      (<= l 0xff)
      (do (write-uint8 w 0xc4)
          (write-uint8 w l)
          (write-bytes w x))
      (<= l 0xffff)
      (do (write-uint8  w 0xc5)
          (write-uint16 w l)
          (write-bytes  w x))
      (<= l 0xffffffff)
      (do (write-uint8  w 0xc6)
          (write-uint32 w l)
          (write-bytes  w x)))))

(defn- pack-bytes-size
  [l]
  (cond
    (<= l 0xff)       (+ 2 l)
    (<= l 0xffff)     (+ 3 l)
    (<= l 0xffffffff) (+ 5 l)))

(defprotocol Packable
  "Objects that can be serialized as MessagePack types"
  (packable-size [this])
  (packable-pack [this writer]))

(defn- pack-sequential-size
  [l xs]
  (let [s (transduce (map packable-size) + 0 xs)]
    (cond
      ;; fixarray
      (<= l 0xf)        (+ 1 s)
      ;; array 16
      (<= l 0xffff)     (+ 3 s)
      ;; array 32
      (<= l 0xffffffff) (+ 5 s))))

(defn- pack-sequential
  [l xs w]
  (cond
    ;; fixarray
    (<= l 0xf)
    (write-uint8 w (bit-or 0x90 l))
    ;; array 16
    (<= l 0xffff)
    (do (write-uint8  w 0xdc)
        (write-uint16 w l))
    ;; array 32
    (<= l 0xffffffff)
    (do (write-uint8  w 0xdd)
        (write-uint32 w l)))
  (doseq [x xs]
    (packable-pack x w)))

(defn- pack-map
  [l m w]
  (cond
    ;; fixmap
    (<= l 0xf)
    (write-uint8 w (bit-or 0x80 l))
    ;; map 16
    (<= l 0xffff)
    (do (write-uint8  w 0xde)
        (write-uint16 w l))
    ;; map 32
    (<= l 0xffffffff)
    (do (write-uint8  w 0xdf)
        (write-uint32 w l)))
  (doseq [x (interleave (keys m) (vals m))]
    (packable-pack x w)))

(extend-protocol Packable

  nil
  (packable-size [_] 1)
  (packable-pack [x w]
    (write-uint8 w 0xc0))

  boolean
  (packable-size [_] 1)
  (packable-pack [x w]
    (if x
      (write-uint8 w 0xc3)
      (write-uint8 w 0xc2)))

  number
  (packable-size [x]
    (if (not= x (Math/floor x)) 9  ;; double
      (cond
        (<= 0 x 127)            1  ;; +fixnum
        (<= -32 x -1)           1  ;; -fixnum
        (<= 0 x 0xff)           2  ;; uint 8
        (<= 0 x 0xffff)         3  ;; uint 16
        (<= 0 x 0xffffffff)     5  ;; uint 32
        (<= -0x80 x -1)         2  ;; int 8
        (<= -0x8000 x -1)       3  ;; int 16
        (<= -0x80000000 x -1)   5  ;; int 32
        :else (throw (js/RangeError (str "Integer value out of bounds: " x))))))
  (packable-pack [x w]
    (if (not= x (Math/floor x))
      (do (write-uint8  w 0xcb) ;; double
          (write-double w x))
      (cond
        ;; +fixnum
        (<= 0 x 127)          (write-uint8 w x)
        ;; -fixnum
        (<= -32 x -1)         (write-uint8 w x)
        ;; uint 8
        (<= 0 x 0xff)         (do (write-uint8 w 0xcc) (write-uint8  w x))
        ;; uint 16
        (<= 0 x 0xffff)       (do (write-uint8 w 0xcd) (write-uint16 w x))
        ;; uint 32
        (<= 0 x 0xffffffff)   (do (write-uint8 w 0xce) (write-uint32 w x))
        ;; int 8
        (<= -0x80 x -1)       (do (write-uint8 w 0xd0) (write-int8  w x))
        ;; int 16
        (<= -0x8000 x -1)     (do (write-uint8 w 0xd1) (write-int16 w x))
        ;; int 32
        (<= -0x80000000 x -1) (do (write-uint8 w 0xd2) (write-int32 w x))
        :else (throw (js/RangeError (str "Integer value out of bounds: " x))))))

  string
  (packable-size [x]
    (let [l (utf8-byte-length x)]
      (cond
        (< l 0x20)        (+ 1 l)
        (< l 0x100)       (+ 2 l)
        (< l 0x10000)     (+ 3 l)
        (< l 0x100000000) (+ 5 l))))
  (packable-pack [x w]
    (let [l (utf8-byte-length x)]
      (condp > l
        ;; fixstr
        0x20
        (do (write-uint8 w (bit-or l 0xa0))
            (utf8-write-str w x))
        ;; str 8
        0x100
        (do (write-uint8 w 0xd9)
            (write-uint8 w l)
            (utf8-write-str w x))
        ;; str 16
        0x10000
        (do (write-uint8 w 0xda)
            (write-uint16 w l)
            (utf8-write-str w x))
        ;; str 32
        0x100000000
        (do (write-uint8 w 0xdb)
            (write-uint32 w l)
            (utf8-write-str w x)))))

  js/ArrayBuffer
  (packable-size [x]
    (pack-bytes-size (.-byteLength x)))
  (packable-pack [x w]
    (pack-bytes (js/Uint8ClampedArray. x) w))

  js/Uint8ClampedArray
  (packable-size [x]
    (pack-bytes-size (.-byteLength x)))
  (packable-pack [x w]
    (pack-bytes (js/Uint8ClampedArray. x) w))

  cljs.core/EmptyList
  (packable-size [xs]
    (pack-sequential-size (count xs) xs))
  (packable-pack [xs w]
    (pack-sequential (count xs) xs w))

  cljs.core/PersistentVector
  (packable-size [xs]
    (pack-sequential-size (count xs) xs))
  (packable-pack [xs w]
    (pack-sequential (count xs) xs w))

  cljs.core/Cons
  (packable-size [xs]
    (pack-sequential-size (count xs) xs))
  (packable-pack [xs w]
    (pack-sequential (count xs) xs w))

  cljs.core/LazySeq
  (packable-size [xs]
    (pack-sequential-size (count xs) xs))
  (packable-pack [xs w]
    (pack-sequential (count xs) xs w))

  cljs.core/PersistentHashMap
  (packable-size [m]
    (pack-sequential-size (count m) (concat (keys m) (vals m))))
  (packable-pack [m w]
    (pack-map (count m) m w))

  cljs.core/PersistentArrayMap
  (packable-size [m]
    (pack-sequential-size (count m) (concat (keys m) (vals m))))
  (packable-pack [m w]
    (pack-map (count m) m w))

  cljs.core/PersistentTreeMap
  (packable-size [m]
    (pack-sequential-size (count m) (concat (keys m) (vals m))))
  (packable-pack [m w]
    (pack-map (count m) m w)))

(declare unpack-data)

(defn- unpack-n
  [^DataViewReader r n]
  (doall (for [_ (range n)] (unpack-data r))))

(defn- unpack-map
  [^DataViewReader r n]
  (apply hash-map (unpack-n r (* 2 n))))

(defn- unpack-data
  [^DataViewReader r]
  (let [t (read-uint8 r)]
    (cond
      ;; nil format family
      ;;
      (= t 0xc0) nil

      ;; bool format family
      ;;
      (= t 0xc2) false
      (= t 0xc3) true

      ;; int format family
      ;;
      (= (bit-and 0xe0 t) 0xe0) (read-int8 (rewind r 1)) ;; neg fixnum
      (= (bit-and 0x80 t) 0x00) t                        ;; pos fixnum
      (= t 0xcc) (read-uint8  r)
      (= t 0xcd) (read-uint16 r)
      (= t 0xce) (read-uint32 r)
      (= t 0xcf)
      (let [high (read-uint32 r)
            low  (read-uint32 r)]
        (+ low (* high 0x100000000)))
      (= t 0xd0) (read-int8  r)
      (= t 0xd1) (read-int16 r)
      (= t 0xd2) (read-int32 r)
      (= t 0xd3)
      (let [high (read-int32 r)
            low  (read-int32 r)]
        (+ low (* high 0x100000000)))

      ;; float format family
      ;;
      (= t 0xca) (read-float  r)
      (= t 0xcb) (read-double r)

      ;; string format family
      ;;
      (= (bit-and 0xe0 t) 0xa0)
      (utf8-read-str r (bit-and 0x1f t))

      (= t 0xd9) (utf8-read-str r (read-uint8  r))
      (= t 0xda) (utf8-read-str r (read-uint16 r))
      (= t 0xdb) (utf8-read-str r (read-uint32 r))

      ;; bin format family
      ;;
      (= t 0xc4) (read-bytes r (read-uint8  r))
      (= t 0xc5) (read-bytes r (read-uint16 r))
      (= t 0xc6) (read-bytes r (read-uint32 r))

      ;; array formats family
      ;;
      (= (bit-and 0xf0 t) 0x90)
      (unpack-n r (bit-and 0x0f t))
      ;; array 16
      (= t 0xdc)
      (unpack-n r (read-uint16 r))
      ;; array 32
      (= t 0xdd)
      (unpack-n r (read-uint32 r))

      ;; map format family
      (= (bit-and 0xf0 t) 0x80)
      (unpack-map r (bit-and 0x0f t))
      ;; map 16
      (= t 0xde)
      (unpack-map r (read-uint16 r))
      ;; map 32
      (= t 0xdf)
      (unpack-map r (read-uint32 r))

      true (throw (js/TypeError (str "Can't decode tag 0x" (.toString t 16)))))))

(defn pack [x]
  (let [buffer (.-buffer (-> x packable-size js/Uint8Array.))
        writer (DataViewWriter. (js/DataView. buffer) 0)]
    (packable-pack x writer)
     buffer))

(defn unpack [^js/ArrayBuffer buffer]
  (let [view   (js/DataView. buffer)
        reader (DataViewReader. view 0)]
    (try
      (unpack-data reader)
      (catch js/RangeError e
        (if (> (.-byteLength buffer) 0)
          (throw e)
          nil)))))
