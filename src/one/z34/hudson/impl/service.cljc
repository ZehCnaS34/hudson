(ns one.z34.hudson.impl.service)

(defrecord Service [workspace config])


;; service map example
'{:name        :api-gateway
  :expose      #{80 8080}
  :depends-on  #{:bill-service}
  :environment {}}



:qualified.service/name ;; => "qualified_service__name"
