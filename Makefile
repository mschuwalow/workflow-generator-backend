
PROJECT_DIR := $(dir $(realpath $(firstword $(MAKEFILE_LIST))))

.PHONY: fmt fmt-nix

fmt: fmt-nix

fmt-nix:
	nixfmt $$(find ${PROJECT_DIR} -name '*.nix')
