apiVersion: v1
kind: Namespace
metadata:
  name: __NAMESPACE__
  labels:
    purpose: segroup8-cloud-native-experiment
---
apiVersion: v1
kind: Secret
metadata:
  name: experiment-secrets
  namespace: __NAMESPACE__
type: Opaque
stringData:
  MYSQL_ROOT_PASSWORD: __MYSQL_ROOT_PASSWORD__
  DB_PASSWORD: __DB_PASSWORD__
  JWT_SECRET: __JWT_SECRET__
  INTERNAL_SERVICE_TOKEN: __INTERNAL_SERVICE_TOKEN__
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql
  namespace: __NAMESPACE__
spec:
  replicas: 1
  selector:
    matchLabels: { app: mysql }
  template:
    metadata:
      labels: { app: mysql }
    spec:
      containers:
        - name: mysql
          image: __MYSQL_IMAGE__
          imagePullPolicy: IfNotPresent
          ports:
            - { name: mysql, containerPort: 3306 }
          env:
            - name: MYSQL_ROOT_PASSWORD
              valueFrom:
                secretKeyRef: { name: experiment-secrets, key: MYSQL_ROOT_PASSWORD }
          readinessProbe:
            exec:
              command: ["sh", "-c", "mysqladmin ping -h 127.0.0.1 -uroot -p$MYSQL_ROOT_PASSWORD --silent"]
            initialDelaySeconds: 10
            periodSeconds: 5
          resources:
            requests: { cpu: 250m, memory: 512Mi }
            limits: { cpu: "1", memory: 1536Mi }
          volumeMounts:
            - name: mysql-init
              mountPath: /docker-entrypoint-initdb.d
              readOnly: true
            - name: mysql-data
              mountPath: /var/lib/mysql
      volumes:
        - name: mysql-init
          hostPath:
            path: __HOST_ROOT__/mysql-init
            type: Directory
        - name: mysql-data
          emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: mysql
  namespace: __NAMESPACE__
spec:
  selector: { app: mysql }
  ports:
    - { name: mysql, port: 3306, targetPort: mysql }
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: monolith
  namespace: __NAMESPACE__
spec:
  replicas: 1
  selector:
    matchLabels: { app: monolith }
  template:
    metadata:
      labels: { app: monolith }
      annotations:
        experiment.segroup8/baseline-image-digest: __MONOLITH_IMAGE_DIGEST__
    spec:
      securityContext: { runAsNonRoot: true, runAsUser: 10001, runAsGroup: 10001, fsGroup: 10001 }
      containers:
        - name: monolith
          image: __BASE_IMAGE__
          imagePullPolicy: IfNotPresent
          ports:
            - { name: http, containerPort: 8080 }
          env:
            - { name: DB_URL, value: "jdbc:mysql://mysql:3306/monolith_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" }
            - { name: DB_USERNAME, value: monolith_app }
            - name: DB_PASSWORD
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: DB_PASSWORD } }
            - name: JWT_SECRET
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: JWT_SECRET } }
            - { name: APP_RISK_AUDIT_LLM_ENABLED, value: "false" }
            - { name: APP_VERSION, value: "experiment-monolith-baseline" }
            - { name: APP_COMMIT, value: "bb72290cff96c78ab189468b82db1f8ba3cd9323" }
            - { name: TZ, value: Asia/Shanghai }
          startupProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 5
            failureThreshold: 36
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: http }
            periodSeconds: 5
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 10
          resources:
            requests: { cpu: 100m, memory: 256Mi }
            limits: { cpu: 500m, memory: 768Mi }
          volumeMounts:
            - { name: uploads, mountPath: /app/uploads }
      volumes:
        - { name: uploads, emptyDir: {} }
---
apiVersion: v1
kind: Service
metadata:
  name: monolith
  namespace: __NAMESPACE__
spec:
  selector: { app: monolith }
  ports:
    - { name: http, port: 8080, targetPort: http }
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: identity-governance-service
  namespace: __NAMESPACE__
spec:
  replicas: 1
  selector:
    matchLabels: { app: identity-governance-service }
  template:
    metadata:
      labels: { app: identity-governance-service }
      annotations:
        experiment.segroup8/jar-sha256: __IDENTITY_JAR_SHA256__
    spec:
      securityContext: { runAsNonRoot: true, runAsUser: 10001, runAsGroup: 10001, fsGroup: 10001 }
      containers:
        - name: identity-governance-service
          image: __BASE_IMAGE__
          imagePullPolicy: IfNotPresent
          command: ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
          ports:
            - { name: http, containerPort: 8091 }
          env:
            - { name: SERVER_PORT, value: "8091" }
            - { name: DB_URL, value: "jdbc:mysql://mysql:3306/identity_governance_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" }
            - { name: DB_USERNAME, value: identity_governance_app }
            - name: DB_PASSWORD
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: DB_PASSWORD } }
            - name: JWT_SECRET
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: JWT_SECRET } }
            - name: INTERNAL_SERVICE_TOKEN
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: INTERNAL_SERVICE_TOKEN } }
            - { name: APP_VERSION, value: "experiment-__RUN_ID__" }
            - { name: APP_COMMIT, value: "__GIT_COMMIT__" }
            - { name: TZ, value: Asia/Shanghai }
          startupProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 5
            failureThreshold: 36
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: http }
            periodSeconds: 5
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 10
          resources:
            requests: { cpu: 100m, memory: 256Mi }
            limits: { cpu: 500m, memory: 768Mi }
          volumeMounts:
            - name: identity-jar
              mountPath: /app/app.jar
              readOnly: true
      volumes:
        - name: identity-jar
          hostPath:
            path: __HOST_ROOT__/jars/identity-governance-service-1.0.0.jar
            type: File
---
apiVersion: v1
kind: Service
metadata:
  name: identity-governance-service
  namespace: __NAMESPACE__
spec:
  selector: { app: identity-governance-service }
  ports:
    - { name: http, port: 8091, targetPort: http }
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: __NAMESPACE__
spec:
  replicas: 1
  selector:
    matchLabels: { app: order-service }
  template:
    metadata:
      labels: { app: order-service }
      annotations:
        experiment.segroup8/jar-sha256: __ORDER_JAR_SHA256__
    spec:
      securityContext: { runAsNonRoot: true, runAsUser: 10001, runAsGroup: 10001, fsGroup: 10001 }
      containers:
        - name: order-service
          image: __BASE_IMAGE__
          imagePullPolicy: IfNotPresent
          command: ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
          ports:
            - { name: http, containerPort: 8085 }
          env:
            - { name: SERVER_PORT, value: "8085" }
            - { name: DB_URL, value: "jdbc:mysql://mysql:3306/order_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" }
            - { name: DB_USERNAME, value: order_app }
            - name: DB_PASSWORD
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: DB_PASSWORD } }
            - name: JWT_SECRET
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: JWT_SECRET } }
            - name: INTERNAL_SERVICE_TOKEN
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: INTERNAL_SERVICE_TOKEN } }
            - { name: OUTBOX_PUBLISHER_ENABLED, value: "false" }
            - { name: APP_VERSION, value: "experiment-__RUN_ID__" }
            - { name: APP_COMMIT, value: "__GIT_COMMIT__" }
            - { name: TZ, value: Asia/Shanghai }
          startupProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 5
            failureThreshold: 36
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: http }
            periodSeconds: 5
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 10
          resources:
            requests: { cpu: 100m, memory: 256Mi }
            limits: { cpu: 500m, memory: 768Mi }
          volumeMounts:
            - name: order-jar
              mountPath: /app/app.jar
              readOnly: true
      volumes:
        - name: order-jar
          hostPath:
            path: __HOST_ROOT__/jars/order-service-1.0.0.jar
            type: File
---
apiVersion: v1
kind: Service
metadata:
  name: order-service
  namespace: __NAMESPACE__
spec:
  selector: { app: order-service }
  ports:
    - { name: http, port: 8085, targetPort: http }
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: secondhand-service
  namespace: __NAMESPACE__
spec:
  replicas: 1
  selector:
    matchLabels: { app: secondhand-service }
  template:
    metadata:
      labels: { app: secondhand-service }
      annotations:
        experiment.segroup8/jar-sha256: __SECONDHAND_JAR_SHA256__
    spec:
      securityContext: { runAsNonRoot: true, runAsUser: 10001, runAsGroup: 10001, fsGroup: 10001 }
      containers:
        - name: secondhand-service
          image: __BASE_IMAGE__
          imagePullPolicy: IfNotPresent
          command: ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
          ports:
            - { name: http, containerPort: 8080 }
          env:
            - { name: SERVER_PORT, value: "8080" }
            - { name: DB_URL, value: "jdbc:mysql://mysql:3306/secondhand_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" }
            - { name: DB_USERNAME, value: secondhand_app }
            - name: DB_PASSWORD
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: DB_PASSWORD } }
            - name: JWT_SECRET
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: JWT_SECRET } }
            - name: INTERNAL_SERVICE_TOKEN
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: INTERNAL_SERVICE_TOKEN } }
            - { name: ORDER_SERVICE_URL, value: "http://order-service:8085" }
            - { name: IDENTITY_SERVICE_URL, value: "http://identity-governance-service:8091" }
            - { name: ORDER_CONNECT_TIMEOUT, value: "500ms" }
            - { name: ORDER_READ_TIMEOUT, value: "3s" }
            - { name: ORDER_MAX_ATTEMPTS, value: "30" }
            - { name: IDENTITY_CONNECT_TIMEOUT, value: "500ms" }
            - { name: IDENTITY_READ_TIMEOUT, value: "3s" }
            - { name: TRADE_RECOVERY_DELAY_MS, value: "2000" }
            - { name: SCHEDULING_POOL_SIZE, value: "2" }
            - { name: APP_VERSION, value: "experiment-__RUN_ID__" }
            - { name: APP_COMMIT, value: "__GIT_COMMIT__" }
            - { name: TZ, value: Asia/Shanghai }
          startupProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 5
            failureThreshold: 36
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: http }
            periodSeconds: 5
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 10
          resources:
            requests: { cpu: 100m, memory: 256Mi }
            limits: { cpu: 500m, memory: 768Mi }
          volumeMounts:
            - name: secondhand-jar
              mountPath: /app/app.jar
              readOnly: true
      volumes:
        - name: secondhand-jar
          hostPath:
            path: __HOST_ROOT__/jars/secondhand-service-1.0.0.jar
            type: File
---
apiVersion: v1
kind: Service
metadata:
  name: secondhand-service
  namespace: __NAMESPACE__
spec:
  selector: { app: secondhand-service }
  ports:
    - { name: http, port: 8080, targetPort: http }
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: catalog-shop-service
  namespace: __NAMESPACE__
spec:
  replicas: 1
  selector:
    matchLabels: { app: catalog-shop-service }
  template:
    metadata:
      labels: { app: catalog-shop-service }
    spec:
      securityContext: { runAsNonRoot: true, runAsUser: 10001, runAsGroup: 10001, fsGroup: 10001 }
      containers:
        - name: catalog-shop-service
          image: __BASE_IMAGE__
          imagePullPolicy: IfNotPresent
          command: ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
          ports:
            - { name: http, containerPort: 8080 }
          env:
            - { name: SERVER_PORT, value: "8080" }
            - { name: DB_URL, value: "jdbc:mysql://mysql:3306/catalog_shop_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" }
            - { name: DB_USERNAME, value: catalog_shop_app }
            - name: DB_PASSWORD
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: DB_PASSWORD } }
            - name: JWT_SECRET
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: JWT_SECRET } }
            - name: INTERNAL_SERVICE_TOKEN
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: INTERNAL_SERVICE_TOKEN } }
            - { name: IDENTITY_STATUS_CHECK_URL, value: "http://identity-governance-service:8091/internal/auth/introspect" }
            - { name: IDENTITY_STATUS_CHECK_MODE, value: introspect }
            - { name: MESSAGING_EVENT_URL, value: "http://messaging:8084" }
            - { name: ORDER_EVENT_URL, value: "http://order-service:8085" }
            - { name: OUTBOX_PUBLISH_MS, value: "2000" }
            - { name: HTTP_CONNECT_TIMEOUT_MS, value: "500" }
            - { name: HTTP_READ_TIMEOUT_MS, value: "1500" }
            - { name: APP_VERSION, value: "experiment-__RUN_ID__" }
            - { name: GIT_COMMIT, value: "__GIT_COMMIT__" }
            - { name: TZ, value: Asia/Shanghai }
          startupProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 5
            failureThreshold: 60
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: http }
            periodSeconds: 5
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 10
          resources:
            requests: { cpu: 100m, memory: 256Mi }
            limits: { cpu: 500m, memory: 768Mi }
          securityContext: { allowPrivilegeEscalation: false, readOnlyRootFilesystem: true }
          volumeMounts:
            - { name: catalog-jar, mountPath: /app/app.jar, readOnly: true }
            - { name: tmp, mountPath: /tmp }
      volumes:
        - name: catalog-jar
          hostPath:
            path: __HOST_ROOT__/jars/catalog-shop-service-1.0.0.jar
            type: File
        - { name: tmp, emptyDir: {} }
---
apiVersion: v1
kind: Service
metadata:
  name: segroup8-catalog-shop
  namespace: __NAMESPACE__
spec:
  selector: { app: catalog-shop-service }
  ports:
    - { name: http, port: 8080, targetPort: http }
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: benefits-finance-service
  namespace: __NAMESPACE__
spec:
  replicas: 1
  selector:
    matchLabels: { app: benefits-finance-service }
  template:
    metadata:
      labels: { app: benefits-finance-service }
    spec:
      securityContext: { runAsNonRoot: true, runAsUser: 10001, runAsGroup: 10001, fsGroup: 10001 }
      terminationGracePeriodSeconds: 35
      containers:
        - name: benefits-finance-service
          image: __BASE_IMAGE__
          imagePullPolicy: IfNotPresent
          command: ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
          ports:
            - { name: http, containerPort: 8085 }
          env:
            - { name: SERVER_PORT, value: "8085" }
            - { name: DB_URL, value: "jdbc:mysql://mysql:3306/benefits_finance_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" }
            - { name: DB_USERNAME, value: benefits_finance_app }
            - name: DB_PASSWORD
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: DB_PASSWORD } }
            - name: JWT_SECRET
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: JWT_SECRET } }
            - name: INTERNAL_SERVICE_TOKEN
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: INTERNAL_SERVICE_TOKEN } }
            - { name: OUTBOX_EVENT_SINK_URL, value: "http://messaging:8084/internal/events" }
            - { name: HTTP_CONNECT_TIMEOUT_MS, value: "500" }
            - { name: HTTP_READ_TIMEOUT_MS, value: "1500" }
            - { name: APP_VERSION, value: "experiment-__RUN_ID__" }
            - { name: APP_COMMIT, value: "__GIT_COMMIT__" }
            - { name: APP_BUILD_TIME, value: experiment }
            - { name: TZ, value: Asia/Shanghai }
          startupProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 5
            failureThreshold: 60
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: http }
            periodSeconds: 5
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 10
          resources:
            requests: { cpu: 100m, memory: 256Mi }
            limits: { cpu: 500m, memory: 768Mi }
          securityContext: { allowPrivilegeEscalation: false, readOnlyRootFilesystem: true }
          volumeMounts:
            - { name: finance-jar, mountPath: /app/app.jar, readOnly: true }
            - { name: tmp, mountPath: /tmp }
      volumes:
        - name: finance-jar
          hostPath:
            path: __HOST_ROOT__/jars/benefits-finance-service-1.0.0.jar
            type: File
        - { name: tmp, emptyDir: {} }
---
apiVersion: v1
kind: Service
metadata:
  name: benefits-finance
  namespace: __NAMESPACE__
spec:
  selector: { app: benefits-finance-service }
  ports:
    - { name: http, port: 8085, targetPort: http }
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: messaging-service
  namespace: __NAMESPACE__
spec:
  replicas: 1
  selector:
    matchLabels: { app: messaging-service }
  template:
    metadata:
      labels: { app: messaging-service }
    spec:
      securityContext: { runAsNonRoot: true, runAsUser: 10001, runAsGroup: 10001, fsGroup: 10001 }
      containers:
        - name: messaging-service
          image: __BASE_IMAGE__
          imagePullPolicy: IfNotPresent
          command: ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
          ports:
            - { name: http, containerPort: 8084 }
          env:
            - { name: SERVER_PORT, value: "8084" }
            - { name: DB_URL, value: "jdbc:mysql://mysql:3306/messaging_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" }
            - { name: DB_USERNAME, value: messaging_app }
            - name: DB_PASSWORD
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: DB_PASSWORD } }
            - name: JWT_SECRET
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: JWT_SECRET } }
            - name: INTERNAL_SERVICE_TOKEN
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: INTERNAL_SERVICE_TOKEN } }
            - name: INTERNAL_OPERATIONS_TOKEN
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: INTERNAL_SERVICE_TOKEN } }
            - { name: IDENTITY_SERVICE_URL, value: "http://identity-governance-service:8091" }
            - name: IDENTITY_SERVICE_TOKEN
              valueFrom: { secretKeyRef: { name: experiment-secrets, key: INTERNAL_SERVICE_TOKEN } }
            - { name: REALTIME_ALLOWED_ORIGIN_PATTERNS, value: "http://localhost:5174" }
            - { name: APP_VERSION, value: "experiment-__RUN_ID__" }
            - { name: APP_COMMIT, value: "__GIT_COMMIT__" }
            - { name: APP_BUILD_TIME, value: experiment }
            - { name: TZ, value: Asia/Shanghai }
          startupProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 5
            failureThreshold: 60
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: http }
            periodSeconds: 5
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            periodSeconds: 10
          resources:
            requests: { cpu: 100m, memory: 256Mi }
            limits: { cpu: 500m, memory: 768Mi }
          securityContext: { allowPrivilegeEscalation: false, readOnlyRootFilesystem: true }
          volumeMounts:
            - { name: messaging-jar, mountPath: /app/app.jar, readOnly: true }
            - { name: tmp, mountPath: /tmp }
      volumes:
        - name: messaging-jar
          hostPath:
            path: __HOST_ROOT__/jars/messaging-service-1.0.0.jar
            type: File
        - { name: tmp, emptyDir: {} }
---
apiVersion: v1
kind: Service
metadata:
  name: messaging
  namespace: __NAMESPACE__
spec:
  selector: { app: messaging-service }
  ports:
    - { name: http, port: 8084, targetPort: http }
