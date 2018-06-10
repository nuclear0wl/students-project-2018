def IMAGE_NAME = "greetings_app"
def CONTAINER_NAME = "greeting_app"
def APP_PORT = "5000"
def HUB_USER = "nuclear0wl"

node {
    stage('Initialize') {
	def dockerHome = tool 'myDocker'
        env.PATH = "${dockerHome}/bin:${env.PATH}"
    }

    stage('Stop old container') {
	try {
	    sh "docker stop $CONTAINER_NAME && docker rm $CONTAINER_NAME"
	    echo "Old version of app was removed"
	} catch (error) { 
	}
    }

    stage('Run new container') {
        withCredentials([usernamePassword(credentialsId: 'postgres', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
	        sh "docker run -e DB_URL=postgresql://$USERNAME:$PASSWORD@database:5432/greetings --network='students-project-2018_default' --name $CONTAINER_NAME -d $HUB_USER/$IMAGE_NAME:${env.PARAM_GIT}"
        }
	echo "Image $IMAGE_NAME:${env.PARAM_GIT} was run successfully"
    }

    stage('Integration tests') {
	run_status = sh(returnStdout: true, script: "docker inspect $CONTAINER_NAME | grep Status")
	if (run_status.indexOf('running') < 0) {
	    echo "New version wasn't started"
	    currentBuild.result = 'FAILED'
	    sh "exit 1"
	} else {
	    echo "Container is running"
	}

	APP_ADDR = sh(returnStdout: true, script: "docker inspect $CONTAINER_NAME --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'").trim()
	status = sh(returnSatus: true, script: "curl --silent --connect-timeout 15 --show-error --fail http://$APP_ADDR:$APP_PORT")
        if (status != 0) {
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

    stage('Mark new version as stable') {
	sh "docker tag $HUB_USER/$IMAGE_NAME:${env.PARAM_GIT} $HUB_USER/$IMAGE_NAME:stable"
	withCredentials([usernamePassword(credentialsId: 'nuclear0wl-dockerhub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            sh "docker login -u $USERNAME -p $PASSWORD"
            sh "docker push $USERNAME/$IMAGE_NAME:stable"
            echo "Image was pushed as stable"
        }
    }
}
