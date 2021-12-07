#!/bin/bash
DB_CONTAINER_NAME="${1:-docker_postgres_1}"
SQL_SCRIPTS_DIR="./sql"
SQL_SCRIPTS=(dcsdba_schema.sql container.sql inventory.sql ex2.sql)
_GREEN_DONE="\e[32mdone\e[0m\n"

printf "Bootstraping database in container: ${DB_CONTAINER_NAME}\n"

CONTAINER_EXISTS=`docker ps | grep ${DB_CONTAINER_NAME} | wc -l`

if [ $CONTAINER_EXISTS -eq "0" ]; then
  printf "Container ${DB_CONTAINER_NAME} does not exist or is not running.\n"
  docker-compose up -d postgres
  printf "Waiting for database launch... "
  sleep 5
  printf ${_GREEN_DONE}
fi

for SCRIPT in ${SQL_SCRIPTS[@]}; do
  SCRIPT_PATH="${SQL_SCRIPTS_DIR}/${SCRIPT}"
  printf "Executing ${SCRIPT_PATH}... "
  if docker exec -i ${DB_CONTAINER_NAME} psql -v ON_ERROR_STOP=1 -U postgres < "${SCRIPT_PATH}" > /dev/null ; then
    printf ${_GREEN_DONE}
  else
    printf "Script ${SCRIPT_PATH} failed. Exiting.\n"
    exit 1
  fi
done

printf "Bootstrap finished successfully.\n"
printf "You can now launch dev stack with: docker-compose up\n"