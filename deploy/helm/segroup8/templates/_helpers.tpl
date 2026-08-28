{{- define "segroup8.labels" -}}
app.kubernetes.io/name: segroup8
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | quote }}
{{- end }}

{{- define "segroup8.selectorLabels" -}}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
