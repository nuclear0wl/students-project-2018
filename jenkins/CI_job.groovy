node {
    stage('Initialize') {
	def dockerHome = tool 'myDocker'
        env.PATH = "${dockerHome}/bin:${env.PATH}"
    }

    stage('Checkout') {
        deleteDir()
        checkout scm
    }

    stage('Unit test') {
	status = sh(returnStatus: true, script: "python3 -m unittest greetings_app/test_selects.py").trim()
	if (status != 0) {
	    currentBuild.result = 'FAILED'
	    sh "echo Unit test was failed"
	    sh "exit ${status}"
	}
    }
}
