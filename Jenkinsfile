node {
    stage 'Checkout'
    checkout changelog: true, poll: true, scm: [
            $class: 'GitSCM',
            branches: [[name: '*/master']],
            browser: [$class: 'GitLab', repoUrl: '<repo-url>', version: '8.6'],
            userRemoteConfigs: [[credentialsId: '<credentials-id>', url: '<git-url>']]
    ]

    stage name: 'Build', concurrency: 1
    gitlabCommitStatus {
        sh "mvn --batch-mode -V -U -e clean verify -DskipITs -Dsurefire.useFile=false"
    }
}