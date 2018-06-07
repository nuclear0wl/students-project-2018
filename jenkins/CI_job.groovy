node {
    stage('Hello') {
	sh "echo Hello"
    }

    stage('World') {
        sh "echo World"
    }
}
