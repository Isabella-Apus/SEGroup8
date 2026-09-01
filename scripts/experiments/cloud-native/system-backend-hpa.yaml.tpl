apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: __HPA_NAME__
  namespace: __NAMESPACE__
  annotations:
    meta.helm.sh/release-name: __HELM_RELEASE__
    meta.helm.sh/release-namespace: __NAMESPACE__
  labels:
    app.kubernetes.io/managed-by: Helm
    experiment.segroup8/scope: complete-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: __DEPLOYMENT__
  minReplicas: __MIN_REPLICAS__
  maxReplicas: __MAX_REPLICAS__
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 0
      selectPolicy: Max
      policies:
        - type: Pods
          value: 2
          periodSeconds: 15
    scaleDown:
      stabilizationWindowSeconds: 60
      selectPolicy: Max
      policies:
        - type: Percent
          value: 50
          periodSeconds: 15
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: __TARGET_CPU__
