
PROJECT_DIR := $(dir $(realpath $(firstword $(MAKEFILE_LIST))))
API_FILE = ${PROJECT_DIR}src/main/resources/api.yaml

.PHONY: fmt fmt-nix fmt-api

fmt: fmt-nix fmt-api

fmt-nix:
	fd --full-path ${PROJECT_DIR} -a -e nix -x nixfmt

fmt-api:
	yq -y -S . ${API_FILE} | sponge ${API_FILE}
