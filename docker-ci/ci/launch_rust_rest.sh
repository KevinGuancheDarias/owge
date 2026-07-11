#!/bin/bash
##
# RUST-BACKEND variant of launch_admin_rest.sh. Compiles the Rust port
# (rust-backend/owge-rest) + the Angular frontend, then mounts them into docker
# images (nginx reverse proxy + the Rust backend) and brings the universe up via
# docker-compose.rust.yml.
#
# It is a drop-in equivalent of launch_admin_rest.sh: SAME arguments, SAME env
# (OWGE_DB_URL / OWGE_DB_USER / OWGE_DB_PASS), SAME published port (8110 + N),
# SAME shared static + per-universe dynamic image dirs, SAME fresh-universe DB
# initialization. The ONLY differences are: it builds the Rust backend instead of
# the Java war/kevinsuite, and runs the Rust docker stack.
#
# @param $1 string Project version, an existing OWGE git tag (e.g. 0.11.8), or the special keyword "master" to build from the current master branch HEAD instead of a tag
# @param $2 string Directory where the static (shared) image files live
# @param $3 string Directory where the dynamic (per-universe) image files live
# @param $4 int    Universe id
# @env   OWGE_DB_URL  host:port/dbname
# @env   OWGE_DB_USER / OWGE_DB_PASS
# @env   [OWGE_WORLD_DIR] required only when the DB is empty (fresh universe init)
# @env   [NO_COMPILE] If set, skip compilation (work in progress, as in the Java script)
#
# NOTE: rust-backend/ is git-untracked, so the `git checkout` (of v$1, or of the
# master branch when "master" is passed) does NOT alter it; the Rust backend is
# built from the current working tree (the checkout only pins the frontend + the
# database schema SQL). The frontend version still tracks the checked-out ref.
#
# @author Kevin Guanche Darias
##
echo -e "\e[34m\e[42mKevin Guanche Darias :: Modern OWGE DevOps & CI/CD :: Universe launching tool (RUST backend)\e[39m\e[49m";

if [ -z "$1" ]; then
	echo "Project version not specified";
	exit 1;
fi

if [ -z "$2" ] ||[ ! -d "$2" ]; then
	echo "Directory for owge-data/static not specified, aborting $2";
	exit 1;
fi

if [ -z "$3" ] || [ ! -d "$3" ]; then
	echo "Directory for owge-data/dynamic not specified, aborting";
	exit 1;
fi

if [ -z "$4" ]; then
        echo "Universe id was NOT specified";
        exit 1;
fi
if ! [ "$4" -eq "$4" ]; then
	echo "FATAL: Universe id MUST be a number";
	exit 1;
fi
. ./lib.sh;

# Re-exec under low CPU/IO priority (Linux) so the build doesn't degrade running universes
lowerHostPriority "$@";

envFailureCheck "OWGE_DB_URL" "$OWGE_DB_URL";
envFailureCheck "OWGE_DB_USER" "$OWGE_DB_USER";
envFailureCheck "OWGE_DB_PASS" "$OWGE_DB_PASS";

if [ "$1" = "master" ]; then
	echo "Special keyword 'master': building from the master branch HEAD instead of a version tag";
	oldBranch=`gitGetCurrentBranch`;
	git checkout master;
else
	if ! gitVersionExists "$1"; then
		exit 1;
	fi
	echo "git checkingout tag v$1";
	oldBranch=`gitGetCurrentBranch`;
	oldDetachedHeadValue=`git config advice.detachedHead`;
	git config advice.detachedHead false;
	git checkout "v$1";
	git config advice.detachedHead "$oldDetachedHeadValue";
fi

##
# node:14 runner (overrides lib.sh's node:8 version — the Angular 11 frontend
# requires Node 14), matching launch_admin_rest.sh.
##
function nodeRun() {
	_targetDirectory="$1";
	if [ ! -d "$_targetDirectory" ]; then
		echo "FATAL, nodeRun failed, no such directory $_targetDirectory, aborting script execution";
		rollback;
	fi
	shift;
	docker run -i --rm `dockerBuildPriorityArgs` --env NG_CLI_ANALYTICS=false --env CI=true --volume "$_targetDirectory"://home/node -w=/home/node node:14 $@
}

##
# Compiles the Angular project (player + admin) — identical to the Java launcher.
#
# @param string $1 Source directory
# @param string $2 Target directory
##
function compileAngularProject () {
	test -d "$1/dist" && rm -r "$1/dist";
	test -d "$1" && log info "Compiling Angular project in $1 to $2";
	if [ -d "$2" ]; then
		log error "FATAL, target directory for angular project alreadt exists, used $2, aborting...";
		rollback;
	fi
	cp -rp "$1" "$2";
	log debug "Running npm install";
	nodeRun "$2" npm install &> /dev/null;
	log debug "Running npm run build";
	nodeRun "$2" npm run build &> /dev/null;
	log debug "Running npm run buildAdmin";
	nodeRun "$2" npm run buildAdmin &> /dev/null
	if [ ! -d "$2/dist" ]; then
		log error "FATAL, Angular compilation failed, aborting script execution";
		rollback;
	fi
}

##
# Compiles the rust backend (owge-rest, release profile) inside a rust:1-bookworm
# container so the produced glibc matches the debian:bookworm-slim runtime image.
# The host cargo registry is mounted (like the maven build mounts ~/.m2) for cache
# reuse. On success the binary is at <dir>/target/release/owge-rest.
#
# @param $1 string rust-backend workspace directory
##
function cargoBuildRust () {
	_dir="$1";
	if [ ! -d "$_dir" ]; then
		log error "FATAL, cargoBuildRust: no such directory $_dir, aborting";
		rollback;
	fi
	log info "Compiling rust backend (cargo build --release --bin owge-rest)";
	mkdir -p "$HOME/.cargo/registry";
	# rustup component add rustfmt: the MysqlTemplate derive macro pipes its
	# generated code through rustfmt and panics when it is missing (current
	# rust:1 images don't ship it by default).
	docker run -i --rm `dockerBuildPriorityArgs` \
		--volume "$_dir":/work \
		--volume "$HOME/.cargo/registry":/usr/local/cargo/registry \
		-w /work rust:1-bookworm \
		sh -c 'rustup component add rustfmt && cargo build --release --bin owge-rest';
	if [ ! -f "$_dir/target/release/owge-rest" ]; then
		log error "FATAL, rust compilation failed, no binary at $_dir/target/release/owge-rest, aborting";
		rollback;
	fi
}

# START program itself

no_db_query=`echo "$OWGE_DB_URL" | cut -d '/' -f 1`/
if OWGE_DB_URL=$no_db_query check_mysql_query_fails "SELECT 1"; then
	log error "Invalid mysql server, or invalid credentials"
	exit 1;
fi
mysql_db=`echo "$OWGE_DB_URL" | cut -d '/' -f 2`;
if OWGE_DB_URL=$no_db_query check_mysql_query_fails "SHOW DATABASES" "grep $mysql_db"; then
	log error "No such database $mysql_db"
	exit 1;
fi
business_dir="$PWD"/../../business;
rust_backend_dir="$PWD"/../../rust-backend;
trap ctrl_c INT
if check_mysql_query_fails "SHOW TABLES"; then
	envFailureCheck "OWGE_WORLD_DIR" "$OWGE_WORLD_DIR" "when the db needs intialization this env var is required";
	if ! [ -d  "$OWGE_WORLD_DIR" ]\
		|| ! [ -f "$OWGE_WORLD_DIR/init.sql" ] || ! [ -d "$OWGE_WORLD_DIR/dynamic" ]; then

		log error "Invalid initialization world directory";
		exit 1;
	fi
	log info "Will create the tables and initialize the world";
	if ! mysql_run < "$business_dir/database/02_schema.sql"; then
		log error "Failed to create database structure";
		mysql_run "SHOW TABLES" | while read line; do
			log debug "Deleting table $line";
			mysql_run "SET FOREIGN_KEY_CHECKS=0; DROP TABLE $line";
		done
		exit 1;
	fi
	log debug "Created the tables";
	if ! mysql_run < "$business_dir/database/04_insert_data.sql"; then
		log error "Failed to insert required content";
		exit 1;
	fi
	log debug "inserted the base content";

	if ! mysql_run < "$OWGE_WORLD_DIR/init.sql"; then
		log error "Insert world content failed =/";
		exit 1;
	fi
	log debug "inserted the world content";
	if ! cp -rp "$OWGE_WORLD_DIR/dynamic/"* $3/; then
		log error "Couldn't copy files from to the dynamic folder";
		exit 1;
	fi
	log debug "copied the content";
fi
trap INT;

targetRoot="/tmp/shit_rust";
if [ -z "$NO_COMPILE" ]; then
	test -d "$targetRoot" && rm -r "$targetRoot";
	mkdir "$targetRoot";
	cargoBuildRust "$rust_backend_dir";
	export OWGE_CI_INSTALL_GAME_REST_BIN="$rust_backend_dir/target/release/owge-rest";
	compileAngularProject "$PWD/../../game-frontend" "$targetRoot/frontend";
else
	echo "Currently NO_COMPILE is buggy, and is work in progress :(  ...... aborting :/";
	rollback;
fi
export OWGE_CI_VERSION="$1";
# START Dockerization things
launcherPath="$PWD";
log debug "Invoke main_reverse_proxy/install.sh";
cd main_reverse_proxy;
chmod +x install.sh;
./install.sh "$targetRoot/frontend";
_err=$?;
cd "$launcherPath";
if [ "$_err" == "0" ]; then
	log info "Executing jenkins (rust) install";
	chmod +x jenkins_install_rust.sh
	OWGE_UNIVERSE_ID="$4" ./jenkins_install_rust.sh "$2" "$3";
else
	log error "Failed to install the frontend to the nginx target directory";
fi
log debug "git checkingout again the previously branch: $oldBranch";
git checkout "$oldBranch";
