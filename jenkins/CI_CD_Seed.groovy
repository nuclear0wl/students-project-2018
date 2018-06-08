def gitUrl = 'https://github.com/nuclear0wl/students-project-2018.git'

pipelineJob("CI_job") {
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url(gitUrl)
			refspec('+refs/tags/*:refs/remotes/origin/tags/*')
                        credentials('nuclear0wl-github')
                    }
                    branch('origin/tags/*.*')
                }
            }
            scriptPath("jenkins/CI_job.groovy")
        }
    }

    triggers {
	scm('*/10 * * * *')
    }
}

pipelineJob("CD_job") {
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url(gitUrl)
			refspec('+refs/tags/*:refs/remotes/origin/tags/*')
                        credentials('nuclear0wl-github')
                    }
                    branch('origin/tags/*.*')
                }
            }
            scriptPath("jenkins/CD_job.groovy")
        }
    }

    triggers {
	upstream('CI_job')
    }
}
