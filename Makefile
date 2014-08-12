all:
		docker rm -f visualizer || true
		docker rmi visualizer || true
		lein immutant war
		docker build -t visualizer .

run:
	docker run --name visualizer --rm -t -i -p 127.0.0.1:8081:8080 -p 127.0.0.1:9991:9990 -p 127.0.0.1:8888:8888 --link keycloak:auth --link provider:provider visualizer 
