(ns one.z34.hudson.impl.workspace
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [babashka.fs :as fs]))

(defrecord Workspace [name base-dir deps config])

(def ^:dynamic *cwd* (fs/path (System/getProperty "user.dir")))
(def ^:dynamic *no-pass* (fs/path (System/getProperty "user.home")))

(defn cwd-ancestors []
  (loop [current *cwd*
         path (list)]
    (if (or (nil? current) (= current *no-pass*))
      path
      (recur (fs/parent current)
             (conj path current)))))


(defn workspace-search-path []
  (->> (cwd-ancestors)
       (mapcat fs/list-dir)))

(defn workspace-files []
  (->> (workspace-search-path)
       (filter (comp (partial = "hudson.edn")
                     fs/file-name))))

(def get-workspace-name (comp fs/file-name fs/parent))

(defn resolve-workspace!
  "recursively traverse up to find a hudson.edn file"
  []
  (if-let [workspace-file (last (workspace-files))]
    (map->Workspace
     {:name     (fs/file-name (fs/parent workspace-file))
      :base-dir (fs/parent workspace-file)
      :deps     (edn/read-string (slurp (fs/file (fs/path (fs/parent workspace-file) "deps.edn"))))
      :config   (edn/read-string (slurp (fs/file workspace-file)))})
    (throw (ex-info "Could not find a workspace" {}))))

(defn update-path [deps alias-name]
  (let [{:keys [paths extra-paths replace-paths]} (-> deps :aliases alias-name)]
    (cond
      paths         (assoc deps :paths paths)
      replace-paths (assoc deps :paths replace-paths)
      extra-paths   (update deps :paths concat extra-paths))))

((fn copy-paths [{:keys [deps config]}]
   (let [
         wanted-aliases (select-keys (:aliases deps) (:aliases config))
         base           (:paths deps)
         ]
     {:wanted-aliases wanted-aliases
      :base           base
      }
     )
   )
 '{:deps {:paths   ["src/common"]
          :deps    {babashka/babashka   {:mvn/version "1.3.176"}
                    selmer/selmer       {:mvn/version "1.12.57"}
                    com.wsscode/pathom3 {:mvn/version "2022.10.19-alpha"}}
          :aliases {:server {:extra-paths ["src/server"]}
                    :run    {}}}

   :config {:name       :violet-city/proxy
            :registry   "10.0.0.2:5000"
            :aliases    [:server]
            :main-alias :run}
   }
 )

(defn copy-paths [{:keys [deps config]}]

  )

(-> (resolve-workspace!)
    (copy-paths))
