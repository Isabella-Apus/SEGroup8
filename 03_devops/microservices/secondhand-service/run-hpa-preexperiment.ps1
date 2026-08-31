[CmdletBinding()]
param(
    [ValidateSet("preexperiment", "formal")]
    [string]$ExperimentType = "preexperiment",
    [ValidateRange(1, 10)]
    [int]$RunNumber = 1,
    [string]$Namespace = "",
    [ValidateRange(1, 10)]
    [int]$MinReplicas = 1,
    [ValidateRange(2, 10)]
    [int]$MaxReplicas = 3,
    [ValidateRange(1, 100)]
    [int]$TargetCpuUtilization = 70,
    [ValidateRange(1, 500)]
    [int]$VUs = 15,
    [string]$Duration = "150s",
    [ValidateRange(1024, 65535)]
    [int]$LocalPort = 18080,
    [ValidateRange(60, 900)]
    [int]$ScaleDownTimeoutSeconds = 300,
    [switch]$InstallMetricsServer,
    [switch]$SkipImageBuild,
    [switch]$KeepNamespace
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$evidenceDir = Join-Path $repoRoot "04_tests\microservices\secondhand-service\evidence\hpa"
$runId = Get-Date -Format "yyyyMMdd-HHmmss"
if (-not $Namespace) {
    $Namespace = "segroup8-hpa-$runId"
}
if ($Namespace -notmatch '^segroup8-hpa-[a-z0-9-]+$') {
    throw "Namespace must start with 'segroup8-hpa-' and contain lowercase letters, numbers, or hyphens only."
}
if ($MaxReplicas -lt $MinReplicas) {
    throw "MaxReplicas must be greater than or equal to MinReplicas."
}

New-Item -ItemType Directory -Force -Path $evidenceDir | Out-Null
$snapshotPath = Join-Path $evidenceDir "$runId-hpa-snapshots.csv"
$resourceLogPath = Join-Path $evidenceDir "$runId-kubectl-resources.log"
$summaryPath = Join-Path $evidenceDir "$runId-$ExperimentType-summary.json"
$k6SummaryPath = Join-Path $evidenceDir "$runId-k6-summary.json"
$k6StdoutPath = Join-Path $evidenceDir "$runId-k6-console.log"
$k6StderrPath = Join-Path $evidenceDir "$runId-k6-error.log"
$portForwardStdoutPath = Join-Path $env:TEMP "$runId-port-forward.log"
$portForwardStderrPath = Join-Path $env:TEMP "$runId-port-forward-error.log"
$renderedPath = Join-Path $env:TEMP "$runId-secondhand-hpa.yaml"
$metricsManifestPath = Join-Path $env:TEMP "metrics-server-v0.9.0.yaml"
$metricsPatchPath = Join-Path $env:TEMP "metrics-server-docker-desktop-patch.json"
if ($SkipImageBuild) {
    $imageTag = "hpa-local"
} else {
    $imageTag = "hpa-$runId"
}
$image = "segroup8/secondhand:$imageTag"
$localImageId = ""
$deployedImageId = ""
$imageImportedToNode = $false
$namespaceCreated = $false
$portForward = $null
$loadJob = $null
$snapshots = [System.Collections.Generic.List[object]]::new()
$peakReplicas = 0
$peakReadyReplicas = 0
$initialReplicas = 0
$initialReadyReplicas = 0
$finalReplicas = 0
$finalReadyReplicas = 0

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Command,
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Command failed with exit code $LASTEXITCODE"
    }
}

function Import-LocalImageForDockerDesktop {
    param([Parameter(Mandatory = $true)][string]$Image)

    $context = (& kubectl config current-context).Trim()
    if ($context -ne "docker-desktop") {
        return $false
    }

    $nodeName = (& kubectl get nodes -o "jsonpath={.items[0].metadata.name}").Trim()
    if ($LASTEXITCODE -ne 0 -or -not $nodeName) {
        throw "Unable to identify the Docker Desktop Kubernetes node."
    }

    $archiveName = "$runId-secondhand-image.tar"
    $archivePath = Join-Path $env:TEMP $archiveName
    $loaderPod = ""
    try {
        $debugOutput = @(& kubectl debug "node/$nodeName" --image=busybox:1.36 `
            --profile=sysadmin -- sleep 600)
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to create the Docker Desktop image loader pod."
        }
        $podMatch = [regex]::Match(($debugOutput -join "`n"), "Creating debugging pod ([a-z0-9-]+)")
        if (-not $podMatch.Success) {
            throw "Unable to parse the image loader pod name from kubectl debug output."
        }
        $loaderPod = $podMatch.Groups[1].Value
        Invoke-Checked kubectl @("wait", "--for=condition=Ready", "pod/$loaderPod", "--timeout=60s") |
            Out-Null
        Invoke-Checked docker @("save", "--output", $archivePath, $Image) | Out-Null

        Push-Location $env:TEMP
        try {
            Invoke-Checked kubectl @(
                "cp", ".\$archiveName", "default/$loaderPod`:/host/tmp/$archiveName"
            ) | Out-Null
        } finally {
            Pop-Location
        }
        Invoke-Checked kubectl @(
            "exec", $loaderPod, "--", "chroot", "/host", "ctr", "-n", "k8s.io",
            "images", "import", "/tmp/$archiveName"
        ) | Out-Null
        $imported = @(& kubectl exec $loaderPod -- chroot /host ctr -n k8s.io `
            images ls "name==docker.io/$Image") -join "`n"
        if ($LASTEXITCODE -ne 0 -or $imported -notmatch [regex]::Escape("docker.io/$Image")) {
            throw "The exact image tag was not imported into the Kubernetes container runtime: $Image"
        }
        return $true
    } finally {
        $previousPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        if ($loaderPod) {
            & kubectl exec $loaderPod -- rm -f "/host/tmp/$archiveName" *> $null
            & kubectl delete pod $loaderPod --wait=false *> $null
        }
        if (Test-Path -LiteralPath $archivePath) {
            Remove-Item -LiteralPath $archivePath -Force
        }
        $ErrorActionPreference = $previousPreference
    }
}

function Test-MetricsApi {
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $condition = & kubectl get apiservice v1beta1.metrics.k8s.io `
        -o "jsonpath={.status.conditions[?(@.type=='Available')].status}" 2>$null
    $available = $LASTEXITCODE -eq 0 -and $condition -eq "True"
    $ErrorActionPreference = $previousPreference
    return $available
}

function Wait-MetricsApi {
    param([int]$TimeoutSeconds = 180)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        if (Test-MetricsApi) {
            $previousPreference = $ErrorActionPreference
            $ErrorActionPreference = "Continue"
            & kubectl top nodes *> $null
            $ready = $LASTEXITCODE -eq 0
            $ErrorActionPreference = $previousPreference
            if ($ready) {
                return
            }
        }
        Start-Sleep -Seconds 5
    } while ((Get-Date) -lt $deadline)
    throw "Kubernetes Metrics API did not become ready within $TimeoutSeconds seconds."
}

function Get-ReplicaCount {
    $raw = & kubectl -n $Namespace get deployment segroup8-secondhand -o "jsonpath={.status.replicas}"
    if ($LASTEXITCODE -ne 0 -or -not $raw) {
        return 0
    }
    return [int]$raw
}

function Get-ReadyReplicaCount {
    $raw = & kubectl -n $Namespace get deployment segroup8-secondhand -o "jsonpath={.status.readyReplicas}"
    if ($LASTEXITCODE -ne 0 -or -not $raw) {
        return 0
    }
    return [int]$raw
}

function Record-Snapshot {
    param([string]$Phase)

    $hpa = (& kubectl -n $Namespace get hpa segroup8-secondhand -o json | ConvertFrom-Json)
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to read secondhand HPA status."
    }
    $currentReplicas = if ($null -ne $hpa.status.currentReplicas) { [int]$hpa.status.currentReplicas } else { 0 }
    $desiredReplicas = if ($null -ne $hpa.status.desiredReplicas) { [int]$hpa.status.desiredReplicas } else { 0 }
    $cpuUtilization = $null
    if ($hpa.status.currentMetrics -and $hpa.status.currentMetrics.Count -gt 0) {
        $metric = $hpa.status.currentMetrics |
            Where-Object { $_.type -eq "Resource" -and $_.PSObject.Properties.Name -contains "resource" } |
            Select-Object -First 1
        if ($metric -and $metric.resource.current.PSObject.Properties.Name -contains "averageUtilization") {
            $cpuUtilization = $metric.resource.current.averageUtilization
        }
    }
    $deploymentReplicas = Get-ReplicaCount
    $readyReplicas = Get-ReadyReplicaCount
    $script:peakReplicas = [Math]::Max(
        $script:peakReplicas,
        [Math]::Max($desiredReplicas, [Math]::Max($currentReplicas, $deploymentReplicas))
    )
    $script:peakReadyReplicas = [Math]::Max($script:peakReadyReplicas, $readyReplicas)
    $snapshots.Add([pscustomobject]@{
            timestamp = (Get-Date).ToString("o")
            phase = $Phase
            currentReplicas = $currentReplicas
            desiredReplicas = $desiredReplicas
            deploymentReplicas = $deploymentReplicas
            readyReplicas = $readyReplicas
            cpuUtilizationPercentage = $cpuUtilization
            targetCpuUtilizationPercentage = $TargetCpuUtilization
        })
    $snapshots | Export-Csv -LiteralPath $snapshotPath -NoTypeInformation -Encoding UTF8

    "[$((Get-Date).ToString('o'))] phase=$Phase" | Add-Content -LiteralPath $resourceLogPath -Encoding UTF8
    (& kubectl -n $Namespace get hpa segroup8-secondhand -o wide 2>&1) |
        ForEach-Object { ([string]$_).TrimEnd() } |
        Add-Content -LiteralPath $resourceLogPath -Encoding UTF8
    (& kubectl -n $Namespace top pods -l app.kubernetes.io/component=secondhand-service 2>&1) |
        ForEach-Object { ([string]$_).TrimEnd() } |
        Add-Content -LiteralPath $resourceLogPath -Encoding UTF8
    (& kubectl -n $Namespace get pods -l app.kubernetes.io/component=secondhand-service -o wide 2>&1) |
        ForEach-Object { ([string]$_).TrimEnd() } |
        Add-Content -LiteralPath $resourceLogPath -Encoding UTF8
}

foreach ($command in "docker", "kubectl") {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "Required command is missing: $command"
    }
}
$helmCommand = Get-Command helm -ErrorAction SilentlyContinue
$helmExecutable = if ($helmCommand) { $helmCommand.Source } else { $null }
if (-not $helmExecutable) {
    $helmExecutable = Get-ChildItem `
        (Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Packages") `
        -Filter helm.exe -Recurse -File -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty FullName -First 1
}
if (-not $helmExecutable) {
    throw "Required command is missing: helm"
}

try {
    if (-not (Test-MetricsApi)) {
        if (-not $InstallMetricsServer) {
            throw "Metrics API is unavailable. Re-run with -InstallMetricsServer on a local test cluster."
        }
        Invoke-WebRequest `
            -Uri "https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.9.0/components.yaml" `
            -OutFile $metricsManifestPath
        Invoke-Checked kubectl @("apply", "-f", $metricsManifestPath)
        $metricsArgs = & kubectl -n kube-system get deployment metrics-server `
            -o "jsonpath={.spec.template.spec.containers[0].args}"
        if ($metricsArgs -notmatch '--kubelet-insecure-tls') {
            $patch = '[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
            [System.IO.File]::WriteAllText($metricsPatchPath, $patch)
            Invoke-Checked kubectl @(
                "-n", "kube-system", "patch", "deployment", "metrics-server",
                "--type=json", "--patch-file", $metricsPatchPath
            )
        }
        Invoke-Checked kubectl @("-n", "kube-system", "rollout", "status", "deployment/metrics-server", "--timeout=180s")
    }
    Wait-MetricsApi

    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & kubectl get namespace $Namespace *> $null
    $namespaceExists = $LASTEXITCODE -eq 0
    $ErrorActionPreference = $previousPreference
    if ($namespaceExists) {
        throw "Namespace already exists: $Namespace"
    }
    Invoke-Checked kubectl @("create", "namespace", $Namespace)
    $namespaceCreated = $true

    if (-not $SkipImageBuild) {
        Invoke-Checked mvn @(
            "-B", "--no-transfer-progress",
            "-f", (Join-Path $repoRoot "microservices\pom.xml"),
            "-pl", "secondhand-service", "-am", "package", "-DskipTests"
        )
        Invoke-Checked docker @(
            "build",
            "-f", (Join-Path $repoRoot "microservices\secondhand-service\Dockerfile"),
            "-t", $image,
            (Join-Path $repoRoot "microservices\secondhand-service")
        )
    }
    $localImageId = (& docker image inspect $image --format "{{.Id}}").Trim()
    if ($LASTEXITCODE -ne 0 -or -not $localImageId) {
        throw "Unable to resolve the exact local image ID for $image."
    }
    if (-not $SkipImageBuild) {
        $imageImportedToNode = Import-LocalImageForDockerDesktop -Image $image
    }

    $rootPassword = [Guid]::NewGuid().ToString("N")
    $dbPassword = [Guid]::NewGuid().ToString("N")
    $jwtSecret = ([Guid]::NewGuid().ToString("N") + [Guid]::NewGuid().ToString("N"))
    $internalToken = [Guid]::NewGuid().ToString("N")

    Invoke-Checked kubectl @(
        "-n", $Namespace, "create", "secret", "generic", "secondhand-mysql-config",
        "--from-literal=MYSQL_ROOT_PASSWORD=$rootPassword",
        "--from-literal=MYSQL_DATABASE=secondhand_db",
        "--from-literal=MYSQL_USER=secondhand_app",
        "--from-literal=MYSQL_PASSWORD=$dbPassword"
    )
    Invoke-Checked kubectl @("-n", $Namespace, "create", "deployment", "mysql", "--image=mysql:8.4.6", "--port=3306")
    Invoke-Checked kubectl @("-n", $Namespace, "set", "env", "deployment/mysql", "--from=secret/secondhand-mysql-config")
    Invoke-Checked kubectl @(
        "-n", $Namespace, "set", "resources", "deployment/mysql",
        "--requests=cpu=100m,memory=256Mi", "--limits=cpu=500m,memory=768Mi"
    )
    Invoke-Checked kubectl @("-n", $Namespace, "expose", "deployment", "mysql", "--port=3306", "--target-port=3306")
    Invoke-Checked kubectl @("-n", $Namespace, "rollout", "status", "deployment/mysql", "--timeout=180s")

    Invoke-Checked kubectl @(
        "-n", $Namespace, "create", "secret", "generic", "segroup8-secondhand-secret",
        "--from-literal=DB_URL=jdbc:mysql://mysql:3306/secondhand_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
        "--from-literal=DB_USERNAME=secondhand_app",
        "--from-literal=DB_PASSWORD=$dbPassword",
        "--from-literal=JWT_SECRET=$jwtSecret",
        "--from-literal=INTERNAL_SERVICE_TOKEN=$internalToken"
    )

    $chart = Join-Path $repoRoot "deploy\helm\segroup8"
    $schemaPath = (Join-Path $repoRoot "backend\src\main\resources\schema.sql").Replace('\', '/')
    $helmArguments = @(
        "template", "segroup8", $chart,
        "--namespace", $Namespace,
        "--show-only", "templates/secondhand-configmap.yaml",
        "--show-only", "templates/secondhand-service.yaml",
        "--show-only", "templates/secondhand-deployment.yaml",
        "--show-only", "templates/secondhand-hpa.yaml",
        "--set-string", "backend.image.repository=registry.example/segroup8/backend",
        "--set-string", "backend.image.tag=sha-local",
        "--set-string", "frontend.image.repository=registry.example/segroup8/frontend",
        "--set-string", "frontend.image.tag=sha-local",
        "--set", "secondhand.enabled=true",
        "--set", "secondhand.autoscaling.enabled=true",
        "--set", "secondhand.autoscaling.minReplicas=$MinReplicas",
        "--set", "secondhand.autoscaling.maxReplicas=$MaxReplicas",
        "--set", "secondhand.autoscaling.targetCPUUtilizationPercentage=$TargetCpuUtilization",
        "--set", "secondhand.autoscaling.behavior.scaleDown.stabilizationWindowSeconds=60",
        "--set-string", "secondhand.image.repository=segroup8/secondhand",
        "--set-string", "secondhand.image.tag=$imageTag",
        "--set-string", "secondhand.image.pullPolicy=Never",
        "--set-string", "secondhand.deployment.version=hpa-$ExperimentType",
        "--set-string", "secondhand.deployment.commit=$(& git -C $repoRoot rev-parse HEAD)",
        "--set-string", "secondhand.deployment.buildTime=$((Get-Date).ToString('o'))",
        "--set-file", "mysql.initSchema=$schemaPath"
    )
    $rendered = & $helmExecutable @helmArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Helm failed to render the secondhand HPA experiment resources."
    }
    [System.IO.File]::WriteAllLines($renderedPath, [string[]]$rendered)
    Invoke-Checked kubectl @("-n", $Namespace, "apply", "-f", $renderedPath)
    Invoke-Checked kubectl @("-n", $Namespace, "rollout", "status", "deployment/segroup8-secondhand", "--timeout=240s")
    $deployedImage = (& kubectl -n $Namespace get deployment segroup8-secondhand `
        -o "jsonpath={.spec.template.spec.containers[0].image}").Trim()
    if ($deployedImage -ne $image) {
        throw "Deployment image mismatch: expected $image, got $deployedImage."
    }
    $deployedImageId = (& kubectl -n $Namespace get pods `
        -l "app.kubernetes.io/component=secondhand-service" `
        -o "jsonpath={.items[0].status.containerStatuses[0].imageID}").Trim()

    $portForwardOptions = @{
        FilePath = "kubectl.exe"
        ArgumentList = @("-n", $Namespace, "port-forward", "service/secondhand-service", "$LocalPort`:8080")
        WindowStyle = "Hidden"
        RedirectStandardOutput = $portForwardStdoutPath
        RedirectStandardError = $portForwardStderrPath
        PassThru = $true
    }
    $portForward = Start-Process @portForwardOptions

    $healthDeadline = (Get-Date).AddSeconds(120)
    do {
        try {
            $health = Invoke-RestMethod -Uri "http://127.0.0.1:$LocalPort/actuator/health/readiness" -TimeoutSec 5
            if ($health.status -eq "UP") {
                break
            }
        } catch {
            Start-Sleep -Seconds 3
        }
    } while ((Get-Date) -lt $healthDeadline)
    if (-not $health -or $health.status -ne "UP") {
        throw "secondhand-service readiness endpoint did not become UP."
    }

    Start-Sleep -Seconds 20
    Record-Snapshot "baseline"
    $initialReplicas = Get-ReplicaCount
    $initialReadyReplicas = Get-ReadyReplicaCount

    $dockerArgs = @(
        "run", "--rm",
        "--add-host", "host.docker.internal:host-gateway",
        "--mount", "type=bind,source=$repoRoot,target=/work,readonly",
        "--mount", "type=bind,source=$evidenceDir,target=/evidence",
        "-w", "/work",
        "-e", "BASE_URL=http://host.docker.internal:$LocalPort/api",
        "-e", "VUS=$VUs",
        "-e", "DURATION=$Duration",
        "grafana/k6:latest", "run", "--quiet",
        "--summary-export", "/evidence/$runId-k6-summary.json",
        "/work/04_tests/performance/k6/secondhand-hpa.k6.js"
    )
    $dockerArgsJson = ConvertTo-Json -InputObject $dockerArgs -Compress
    $loadJob = Start-Job -ScriptBlock {
        param(
            [string]$CommandArgumentsJson,
            [string]$StandardOutputPath,
            [string]$StandardErrorPath
        )
        $parsedArguments = ConvertFrom-Json -InputObject $CommandArgumentsJson
        [string[]]$CommandArguments = @()
        foreach ($argument in $parsedArguments) {
            $CommandArguments += [string]$argument
        }
        & docker.exe @CommandArguments 1> $StandardOutputPath 2> $StandardErrorPath
        [pscustomobject]@{ ExitCode = [int]$LASTEXITCODE }
    } -ArgumentList $dockerArgsJson, $k6StdoutPath, $k6StderrPath
    while ($loadJob.State -in @("NotStarted", "Running")) {
        Record-Snapshot "load"
        Start-Sleep -Seconds 5
    }
    $loadJob | Wait-Job | Out-Null
    $loadResult = Receive-Job -Job $loadJob
    $loadExitCode = $loadResult.ExitCode
    foreach ($logPath in @($k6StdoutPath, $k6StderrPath)) {
        if (Test-Path -LiteralPath $logPath) {
            [string[]]$logLines = @(Get-Content -LiteralPath $logPath)
            [System.IO.File]::WriteAllLines(
                $logPath,
                $logLines,
                [System.Text.UTF8Encoding]::new($false)
            )
        }
    }
    if ($null -eq $loadExitCode) {
        throw "k6 load job did not return an exit code."
    }
    if ($loadExitCode -ne 0) {
        throw "k6 load failed with exit code $loadExitCode."
    }
    $k6Summary = Get-Content -LiteralPath $k6SummaryPath -Raw | ConvertFrom-Json
    $httpSummary = [ordered]@{
        requests = [int]$k6Summary.metrics.http_reqs.count
        requestsPerSecond = [math]::Round([double]$k6Summary.metrics.http_reqs.rate, 3)
        averageDurationMs = [math]::Round([double]$k6Summary.metrics.http_req_duration.avg, 2)
        p95DurationMs = [math]::Round([double]$k6Summary.metrics.http_req_duration.'p(95)', 2)
        failedRate = [double]$k6Summary.metrics.http_req_failed.value
        businessSuccessRate = [double]$k6Summary.metrics.business_success.value
        serverErrorRate = [double]$k6Summary.metrics.server_error.value
    }
    Record-Snapshot "load-complete"

    $scaleDownDeadline = (Get-Date).AddSeconds($ScaleDownTimeoutSeconds)
    do {
        Record-Snapshot "recovery"
        $finalReplicas = Get-ReplicaCount
        $finalReadyReplicas = Get-ReadyReplicaCount
        if ($finalReplicas -le $MinReplicas -and $finalReadyReplicas -le $MinReplicas) {
            break
        }
        Start-Sleep -Seconds 10
    } while ((Get-Date) -lt $scaleDownDeadline)
    Record-Snapshot "final"
    $finalReplicas = Get-ReplicaCount
    $finalReadyReplicas = Get-ReadyReplicaCount

    $summary = [ordered]@{
        executedAt = (Get-Date).ToString("o")
        experimentType = $ExperimentType
        runNumber = $RunNumber
        status = if (
            $peakReplicas -gt $initialReplicas -and
            $peakReadyReplicas -gt $initialReadyReplicas -and
            $finalReplicas -le $MinReplicas -and
            $finalReadyReplicas -le $MinReplicas
        ) { "PASS" } else { "FAIL" }
        clusterContext = (& kubectl config current-context)
        kubernetesVersion = (& kubectl version -o json | ConvertFrom-Json).serverVersion.gitVersion
        metricsServer = "v0.9.0"
        namespace = $Namespace
        serviceImage = $image
        localImageId = $localImageId
        deployedImageId = $deployedImageId
        imageImportedToNode = $imageImportedToNode
        gitCommit = (& git -C $repoRoot rev-parse HEAD)
        hpa = [ordered]@{
            minReplicas = $MinReplicas
            maxReplicas = $MaxReplicas
            targetCpuUtilizationPercentage = $TargetCpuUtilization
            initialReplicas = $initialReplicas
            initialReadyReplicas = $initialReadyReplicas
            peakReplicas = $peakReplicas
            peakReadyReplicas = $peakReadyReplicas
            allPeakReplicasReady = ($peakReadyReplicas -ge $peakReplicas)
            finalReplicas = $finalReplicas
            finalReadyReplicas = $finalReadyReplicas
        }
        load = [ordered]@{
            vus = $VUs
            duration = $Duration
            summary = (Split-Path $k6SummaryPath -Leaf)
        }
        http = $httpSummary
        evidence = @(
            (Split-Path $snapshotPath -Leaf),
            (Split-Path $resourceLogPath -Leaf),
            (Split-Path $k6StdoutPath -Leaf),
            (Split-Path $k6StderrPath -Leaf)
        )
    }
    $summary | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $summaryPath -Encoding UTF8

    if ($peakReplicas -le $initialReplicas) {
        throw "HPA did not scale above the initial replica count."
    }
    if ($peakReadyReplicas -le $initialReadyReplicas) {
        throw "HPA created replicas, but no additional secondhand-service pod became ready."
    }
    if ($finalReplicas -gt $MinReplicas) {
        throw "HPA did not return to MinReplicas within $ScaleDownTimeoutSeconds seconds."
    }
    if ($finalReadyReplicas -gt $MinReplicas) {
        throw "Ready secondhand-service pods did not return to MinReplicas within $ScaleDownTimeoutSeconds seconds."
    }
    Write-Host "HPA $ExperimentType run $RunNumber passed. Evidence: $summaryPath"
} finally {
    if ($loadJob) {
        if ($loadJob.State -eq "Running") {
            Stop-Job -Job $loadJob -ErrorAction SilentlyContinue
        }
        Remove-Job -Job $loadJob -Force -ErrorAction SilentlyContinue
    }
    if ($portForward -and -not $portForward.HasExited) {
        Stop-Process -Id $portForward.Id -Force -ErrorAction SilentlyContinue
    }
    if ($namespaceCreated -and -not $KeepNamespace) {
        & kubectl delete namespace $Namespace --wait=false *> $null
    }
    Remove-Item -LiteralPath $renderedPath -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $metricsPatchPath -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $portForwardStdoutPath -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $portForwardStderrPath -Force -ErrorAction SilentlyContinue
}
