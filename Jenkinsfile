node {
    stage name:'Checkout'
    checkout scm

    stage name: 'Build', concurrency: 1
    sh "mvn --batch-mode -P gpg-release -X -V -U -e clean verify -DskipITs -Dsurefire.useFile=false"
}