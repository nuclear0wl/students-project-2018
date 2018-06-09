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
        sh "docker stop $CONTAINER_NAME && docker rm $CONTAINER_NAME"
        echo "Old version of app was removed"
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
    }

    stage('Remove old version') {
	sh "docker tag $HUB_USER/$IMAGE_NAME:${env.PARAM_GIT} $HUB_USER/$IMAGE_NAME:stable"
	withCredentials([usernamePassword(credentialsId: 'nuclear0wl-dockerhub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            sh "docker login -u $USERNAME -p $PASSWORD"
            sh "docker push $USERNAME/$IMAGE_NAME:stable"
            echo "Image was pushed as stable"
        }
    }
}
