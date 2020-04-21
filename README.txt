# ftcp-telemetry-audit

## Baking Changes
* [Changes](https://github.com/autonomic-ai/ftcp-telemetry-audit/compare/master...rc) - The following are the changes ready to be released to prod.

## Using the service

To use this service, you must have a valid client id with permissions:
- resource: `aui:telemetryaudit:audit`
- action: `telemetryaudit:getAudit`

You may add a filter type to show all signals processed, connection status alerts, or telemetry.

Access the service at `https://api.{{hostname}}/v1/audit/{VIN}?type=[all|connectivity|telemetry]` using curl or your favourite rest client.

Example curl:
```
curl 'https://api.dev.instrumented-mile.com/v1/audit/AU7900509CMD003E9?type=all' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {accessToken}'
```

Example response:
```
{
	vin: "AU7900509CMD003E9",
	stages: {
		FTCP_CONVERTER_CONNECTION_STATUS: {
			lastMessageTime: "2020-02-19T21:33:43.417",
			resultDetails: {
				result: "success",
				esn: "CMD003E9",
				errorReason: "Skipped with success - Skipping processing of connection status message",
				metricsConverted: 0,
				messageType: "TCU_CONNECTION_STATUS_ALERT",
				rawFtcpMessageName: "TCU_CONNECTION_STATUS_ALERT"
			}
		},
		FTCP_DECRYPTOR_INGEST_CONNECTION_STATUS: {
			lastMessageTime: "2020-02-19T21:33:43.417",
			resultDetails: {
				result: "success",
				esn: "CMD003E9",
				rawFtcpMessageName: "TCU_CONNECTION_STATUS_ALERT",
				deploymentName: "ftcp-decryptor-telemetry-2xp"
			}
		},
		FTCP_DECRYPTOR_RAW_DECRYPTED_CONNECTION_STATUS: {
			lastMessageTime: "2020-02-19T21:33:43.417",
			resultDetails: {
				result: "success",
				esn: "CMD003E9",
				messageType: "TCU_CONNECTION_STATUS_ALERT",
				rawFtcpMessageName: "TCU_CONNECTION_STATUS_ALERT",
				deploymentName: "ftcp-decryptor-telemetry-2xp"
			}
		},
		FTCP_INGEST: {
			lastMessageTime: "2020-02-19T21:37:42.721",
			resultDetails: {
				result: "success",
				ingestSource: "mqtt-ingest"
			}
		},
		FTCP_CONVERTER: {
			lastMessageTime: "2020-02-19T21:37:42.721",
			resultDetails: {
				result: "success",
				esn: "CMD003E9",
				ftcpVersion: "5.0.9",
				canDB: "B_v19.04A",
				busArch: "3",
				metricsConverted: 1,
				messageType: "SDN_QUERY",
				rawFtcpMessageName: "SDN_QUERY",
				messageTimestamp: "2020-02-19T21:37:41.000000",
				vehicleAssetId: "vehicleAssetId",
				deviceAssetId: "deviceAssetId"
			}
		},
		FTCP_DECRYPTOR_INGEST: {
			lastMessageTime: "2020-02-19T21:37:42.721",
			resultDetails: {
				result: "success",
				esn: "CMD003E9",
				rawFtcpMessageName: "SDN_QUERY",
				deploymentName: "ftcp-decryptor-telemetry-2xp"
			}
		},
		FTCP_DECRYPTOR_RAW_DECRYPTED: {
			lastMessageTime: "2020-02-19T21:37:42.721",
			resultDetails: {
				result: "success",
				esn: "CMD003E9",
				messageType: "SDN_QUERY",
				rawFtcpMessageName: "SDN_QUERY",
				deploymentName: "ftcp-decryptor-telemetry-2xp"
			}
		}
	}
}
```

  
## Tools
* [Helm](https://github.com/autonomic-ai/tms-charts/tree/master/ftcp-telemetry-audit/ftcp-telemetry-audit) - A tool for managing Kubernetes charts
* [Grafana](https://grafana.au-infrastructure.com/dashboard/db/ftcp-telemetry-audit) - An open-source monitoring and graphing tool

## Pipeline
* [Pipeline Values](https://github.com/autonomic-ai/ci-pipelines/blob/master/pipelines/ftcp-telemetry-audit/pipeline-values.yml) - Values for pipeline configuration
* [Concourse](https://ci.tools.k8s.au-infrastructure.com/teams/main/pipelines/ftcp-telemetry-audit) - Concourse pipeline UI
