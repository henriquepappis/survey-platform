SHELL := /bin/bash

.PHONY: dev dev-h2 test docker-up docker-down docker-logs fly-deploy

dev:
	@mvn spring-boot:run

dev-h2:
	@mvn spring-boot:run -Dspring-boot.run.profiles=dev

test:
	@mvn test

docker-up:
	@docker compose up -d

docker-down:
	@docker compose down

docker-logs:
	@docker compose logs -f

fly-deploy:
	@./scripts/fly-deploy.sh $(ARGS)
