# Elasticsearch

Follow the [tutorial](https://www.elastic.co/guide/en/cloud-on-k8s/current/k8s-quickstart.html)

Install ECK types with:

kubectl create -f https://download.elastic.co/downloads/eck/2.5.0/crds.yaml

Start the operator:

kubectl apply -f https://download.elastic.co/downloads/eck/2.5.0/operator.yaml

