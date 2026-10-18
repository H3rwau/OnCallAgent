PYTHON ?= python3
ENV_FILE ?= $(CURDIR)/.env
DOCS_DIR ?=
REQUIRED_DOCS_DIR = $(if $(strip $(DOCS_DIR)),$(DOCS_DIR),$(error 请指定文档目录，例如 make $@ DOCS_DIR=/path/to/docs))
SKIP_LOCAL_MCP ?= 0
KNOWLEDGE_KIND ?= all
SERVICES = $(PYTHON) scripts/local_services.py
COMPOSE = docker compose --env-file "$(ENV_FILE)" -f docker-compose.milvus.yml
export ONCALL_ENV_FILE = $(ENV_FILE)

.PHONY: help deploy preflight init up down status start dev stop restart check wait backend-start backend-stop backend-restart frontend-start frontend-stop frontend-restart frontend-check mcp-start mcp-stop mcp-wait mcp-ensure upload list-docs knowledge-list knowledge-import knowledge-package package frontend-build build run-jar test
.DEFAULT_GOAL := help

help:
	@printf '%s\n' 'make deploy     构建并启动前端、后端 JAR 和 MCP，验证页面及 API' 'make start      启动已构建的前端、后端 JAR 和 MCP，等待就绪' 'make restart    重启前端、后端 JAR，确保 MCP 可用' 'make stop       停止本脚本管理的前端、后端及 MCP，保留外部进程' 'make dev        启动 Vite 热更新前端和 Maven 后端' 'make status     查看前端、后端和 MCP 状态' 'make up/down    单独启动或停止本地 Milvus（数据保留）' 'make build      构建后端 JAR 与 frontend/dist' 'make frontend-start/frontend-stop  管理构建后的前端服务' 'make backend-start/backend-stop    管理后端 JAR' 'make mcp-start/mcp-stop  管理本地 SLS MCP' 'make upload DOCS_DIR=/path/to/docs     上传自己选择的文档' 'make list-docs DOCS_DIR=/path/to/docs  预览文档' 'make knowledge-list    校验并预览项目知识资源（案例和基础参考）' 'make knowledge-import  将项目离线知识资源导入指定后端' 'make knowledge-package  打包独立可迁移的文档和 Python 导入脚本' 'make test       本地回归测试' '可指定 SERVER_PORT=19900 WEB_PORT=18080；其他 MCP：SKIP_LOCAL_MCP=1'

preflight:
	bash scripts/run-agent.sh --check-env
	$(SERVICES) preflight agent
	$(SERVICES) preflight web

# Keep lifecycle steps sequential even when make is invoked with -j.
deploy:
	$(MAKE) preflight
	$(MAKE) build
	$(MAKE) restart

start:
	$(MAKE) preflight
	$(MAKE) mcp-ensure
	$(MAKE) backend-start
	$(MAKE) frontend-start

init: start

restart:
	$(MAKE) preflight
	$(MAKE) mcp-ensure
	$(MAKE) backend-restart
	$(MAKE) frontend-restart

dev:
	$(MAKE) preflight
	$(MAKE) mcp-ensure
	$(SERVICES) start agent --mode dev
	$(SERVICES) start web --mode dev

stop:
	$(MAKE) frontend-stop
	$(MAKE) backend-stop
	$(MAKE) mcp-stop

up:
	$(COMPOSE) up -d

down:
	$(COMPOSE) stop

status:
	$(SERVICES) status agent
	$(SERVICES) status mcp
	$(SERVICES) status web

backend-start:
	$(SERVICES) start agent --mode jar

backend-stop:
	$(SERVICES) stop agent

backend-restart:
	$(SERVICES) restart agent --mode jar

frontend-start:
	$(SERVICES) start web --mode preview

frontend-stop:
	$(SERVICES) stop web

frontend-restart:
	$(SERVICES) restart web --mode preview

frontend-check:
	$(SERVICES) check web

check:
	$(SERVICES) check agent
	$(SERVICES) check web

wait:
	$(SERVICES) wait agent
	$(SERVICES) wait web

mcp-start:
	$(SERVICES) start mcp --timeout 60

mcp-stop:
	$(SERVICES) stop mcp

mcp-wait:
	$(SERVICES) wait mcp --timeout 60

mcp-ensure:
	@if [ "$(SKIP_LOCAL_MCP)" != "1" ]; then $(SERVICES) ensure mcp --timeout 60; fi

upload:
	@api_url="$$($(SERVICES) config api-url)" && $(PYTHON) scripts/upload_docs.py --directory "$(REQUIRED_DOCS_DIR)" --api-url "$$api_url"

list-docs:
	$(PYTHON) scripts/upload_docs.py --directory "$(REQUIRED_DOCS_DIR)" --dry-run

knowledge-list:
	$(PYTHON) scripts/import_knowledge.py --dry-run --kind "$(KNOWLEDGE_KIND)"

knowledge-import:
	@api_url="$$($(SERVICES) config api-url)" && $(PYTHON) scripts/import_knowledge.py --api-url "$$api_url" --kind "$(KNOWLEDGE_KIND)"

knowledge-package:
	$(PYTHON) scripts/import_knowledge.py --package target/omr-knowledge.zip

package:
	mvn -DskipTests package

frontend-build:
	npm --prefix frontend ci
	npm --prefix frontend run build

build: package frontend-build

run-jar:
	@backend_port="$$($(SERVICES) config backend-port)" && bash scripts/run-agent.sh --jar --server.port="$$backend_port"

test:
	mvn -Dtest=ChatModelConfigurationTest,DashScopeHttpTimeoutTest,FileUploadControllerTest,ToolSchemaCompatibilityTest,QueryMetricsConfigurationTest,LogIntegrationTest,ChatRetrievalContextTest,InternalDocsLookupTest,ExactKnowledgeLookupTest test
	$(PYTHON) -m unittest discover -s scripts/tests -v
