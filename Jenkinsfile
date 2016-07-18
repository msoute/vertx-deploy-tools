node {
    stage name: 'Build', concurrency: 1
    gitlabCommitStatus {
        sh "mvn --batch-mode -V -U -e clean verify -DskipITs -Dsurefire.useFile=false"
    }
}