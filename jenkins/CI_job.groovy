def CONTAINER_NAME = "greetings_app"
def HUB_USER = "nuclear0wl"

node {
    stage('Initialize') {
        if (sh(returnStatus: true, script: "which docker")) {
            def dockerHome = tool 'myDocker'
            env.PATH = "${dockerHome}/bin:${env.PATH}"
        }
    }

    stage('Checkout') {
        deleteDir()
        checkout scm
    }

    stage('Build') {
        TAG = sh(returnStdout: true, script: "git tag --contains").trim()
        sh "docker build -t $HUB_USER/$CONTAINER_NAME:$TAG -t $HUB_USER/$CONTAINER_NAME --pull --no-cache ."
        echo "Image $CONTAINER_NAME:$TAG was builded successfully"
    }

    stage('Unit test') {
        status = sh(returnStdout:true, script: "docker run --rm --entrypoint bash $HUB_USER/$CONTAINER_NAME:$TAG -c 'pip -q install mock && python3 greetings_app/test_selects.py 2> dev/null && echo \$?'").trim()
        if (status != "0") {
            echo "STATUS ${status}"
            currentBuild.result = 'FAILED'
            echo "Unit tests were failed"
            sh "exit ${status}"
        } else {
            echo "Unit tests were passed"
        }
    }

    stage('Push to Docker Registry') {
        withCredentials([usernamePassword(credentialsId: 'nuclear0wl-dockerhub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            sh "docker login -u $USERNAME -p $PASSWORD"
            sh "docker push $USERNAME/$CONTAINER_NAME:$TAG"
            sh "docker push $USERNAME/$CONTAINER_NAME:latest"
            echo "Image push complete"
        }
    }
}
