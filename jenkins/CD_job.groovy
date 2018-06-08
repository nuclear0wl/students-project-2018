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
	        sh "docker run -e DB_URL=postgresql://$USERNAME:$PASSWORD@database:5432/greetings --network='students-project-2018_default' --name $CONTAINER_NAME -d $HUB_USER/$IMAGE_NAME:0.12"
	    }
	    echo "Image $CONTAINER_NAME:0.12 was builded successfully"
    }
}
