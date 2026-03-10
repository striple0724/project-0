$env:SERVER_PORT='19101'
$env:SPRING_DATASOURCE_URL='jdbc:h2:mem:taxworkbench_verify;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
./run-gradle.ps1 bootRun
