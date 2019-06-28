

## (Optional) Deploy an Elasticsearch on Cloud Foundry

**Don't use in production!**

```
cf push -f dev/elasticsearch.yml
```

## (Optional) Deploy Kibana on Cloud Foundry

```
cf push -f dev/kibana.yml --no-start
cf add-network-policy kibana --destination-app elasticsearch --protocol tcp --port 9200
cf start kibana
```

## Deploy

```
./mvnw clean package
cf push --var elasticsearch_url=http://elasticsearch.apps.internal:9200 --no-start
# If you are using Elasticsearch on CF
cf add-network-policy syslog-to-elasticsearch --destination-app elasticsearch --protocol tcp --port 9200
cf start syslog-to-elasticsearch

SPACE_NAME=playground
APP_NAME=syslog-to-elasticsearch
TCP_DOMAIN=tcp.apps.pcfone.io
TCP_PORT=10014
APP_PORT=1514

cf create-route ${SPACE_NAME} ${TCP_DOMAIN} --port ${TCP_PORT}
# in case that you can't find an available port,
# cf create-route ${TCP_DOMAIN} --random-port

APP_GUID=$(cf app ${APP_NAME} --guid)
ROUTE_GUID=$(cf curl /v2/routes?q=port:${TCP_PORT} | jq -r .resources[0].metadata.guid)

cf curl /v2/apps/${APP_GUID} -X PUT -d "{\"ports\": [8080, ${APP_PORT}]}"
cf curl /v2/route_mappings -X POST -d "{\"app_guid\": \"${APP_GUID}\", \"route_guid\": \"${ROUTE_GUID}\", \"app_port\": ${APP_PORT}}"
```

Check whether the route mapping is correct.

```
cf curl /v2/apps/${APP_GUID}/route_mappings
```

## Create a user-provided-service

```
cf create-user-provided-service syslog-to-elasticsearch -l syslog://${TCP_DOMAIN}:${TCP_PORT}
```

```
cf bind-service your-app syslog-to-elasticsearch
```