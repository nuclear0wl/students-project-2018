def IMAGE_NAME = "greetings_app"
def CONTAINER_NAME = "greeting_app"
def APP_PORT = "5000"
def HUB_USER = "nuclear0wl"

node {
    // last days docker loader doesnt't work correctly
    // so I decided to use script installation in Docker Global Tool
    // in cause of that I have this Initialize step
    stage('Initialize') {
        if (sh(returnStatus: true, script: "which docker")) {
            def dockerHome = tool 'myDocker'
            env.PATH = "${dockerHome}/bin:${env.PATH}"
        }
    }

    stage('Remove old container') {
        try {
            sh "docker stop $CONTAINER_NAME && docker rm $CONTAINER_NAME"
            echo "Old version of app was removed"
        } catch (error) { 
        }
    }

    // container'll be put into docker-compose stack provided in root of repo
    // I don't see the needness in separated pull step
    stage('Run new container') {
        withCredentials([usernamePassword(credentialsId: 'postgres', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            sh "docker run -e DB_URL=postgresql://$USERNAME:$PASSWORD@database:5432/greetings --network='students-project-2018_default' --name $CONTAINER_NAME -d $HUB_USER/$IMAGE_NAME:${env.PARAM_GIT}"
        }
        echo "Image $IMAGE_NAME:${env.PARAM_GIT} was run successfully"
    }

    stage('Integration tests') {
        run_status = sh(returnStdout: true, script: "docker inspect --format='{{.State.Status}}' $CONTAINER_NAME").trim()
        if (run_status.compareTo('running')) {
            echo "New version wasn't started"
            currentBuild.result = 'FAILED'
            sh "exit 1"
        } else {
            echo "Container is running"
        }
        sleep 5

        APP_ADDR = sh(returnStdout: true, script: "docker inspect $CONTAINER_NAME --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'").trim()
        status = sh(returnStatus: true, script: "curl --silent --connect-timeout 15 --show-error --fail http://$APP_ADDR:$APP_PORT")
        if (status) {
            echo "Can't connect to app"
            currentBuild.result = 'FAILED'
            sh "exit 2"
        } else {
            echo "$APP_ADDR:$APP_PORT avaliable"
        }

        output = sh(returnStdout: true, script: "curl --silent --connect-timeout 15 --show-error --fail http://$APP_ADDR:$APP_PORT")
        if (output.indexOf('Greetings, stranger!') < 0) {
            echo "Unexpected context of app"
            currentBuild.result = 'FAILED'
            sh "exit 3"
        } else {
            echo "Context of app was approved"
        }
    }

    // stable tag can be useful in emegrency situations
    stage('Mark new version as stable') {
        if (!env.PARAM_GIT.compareTo('latest')) {
            sh "docker tag $HUB_USER/$IMAGE_NAME:${env.PARAM_GIT} $HUB_USER/$IMAGE_NAME:stable"
            withCredentials([usernamePassword(credentialsId: 'nuclear0wl-dockerhub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                sh "docker login -u $USERNAME -p $PASSWORD"
                sh "docker push $USERNAME/$IMAGE_NAME:stable"
                echo "Image was pushed as stable"
            }
        }
    }
}
