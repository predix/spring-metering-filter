// Change Snapshot to your own DevCloud Artifactory repo name
def Snapshot = 'PROPEL'
library "security-ci-commons-shared-lib"
def NODE = nodeDetails("java")
// Define DevCloud Artifactory for publishing non-docker image artifacts
def artUploadServer = Artifactory.server('devcloud')

// Change Snapshot to your own DevCloud Artifactory repo name
def Snapshot = 'PROPEL'


pipeline {
    options {
        buildDiscarder(logRotator(artifactDaysToKeepStr: '1', artifactNumToKeepStr: '1', daysToKeepStr: '5', numToKeepStr: '10'))
    }
    agent {
        docker {
            image "${NODE['IMAGE']}"
            label "${NODE['LABEL']}"
        }
    }
    stages {
        stage('Test and Deploy Extensions') {
            image 'maven:3.5'
            label 'dind'
            args '-v /root/.m2:/root/.m2'
        }
    }
    stages {
        stage('Test and deploy extensions') {
            steps {
                checkout scm
                dir('spring-filters-config') {
                    git branch: 'master', changelog: false, credentialsId: 'github.build.ge.com', poll: false, url: 'https://github.build.ge.com/predix/spring-filters-config.git'
                }
                sh '''#!/bin/bash -ex
                    source spring-filters-config/set-env-metering-filter.sh
                    unset NON_PROXY_HOSTS
                    unset HTTPS_PROXY_PORT
                    unset HTTPS_PROXY_HOST

                    mvn clean verify -s spring-filters-config/mvn_settings_noproxy.xml
                '''
            }
            post {
                always {
                    // Test reports
                    junit '**/surefire-reports/junitreports/TEST*.xml'
                    step([$class: 'JacocoPublisher', maximumBranchCoverage: '90', maximumInstructionCoverage: '90'])
                }
            }
        }
<<<<<<< HEAD
        stage('Publish Artifacts') {
            when {
                expression { env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'develop' }
            }
            environment {
                DEPLOY_CREDS = credentials('uaa-predix-artifactory-upload-credentials')
                MAVEN_CENTRAL_STAGING_PROFILE_ID=credentials('MAVEN_CENTRAL_STAGING_PROFILE_ID')
            }
            steps {
                script {
                    if (env.BRANCH_NAME == 'master') {
                    echo 'Branch is master push to MAAXA-MVN and maven central'
                        sh '''#!/bin/bash -ex
                            apk update
                            apk add --no-cache gnupg
                            gpg --version
                            ln -s ${WORKSPACE} /working-dir

                            mvn clean deploy -B -s spring-filters-config/mvn_settings_noproxy.xml \\
                            -DaltDeploymentRepository=artifactory.releases::default::https://devcloud.swcoe.ge.com/artifactory/MAAXA-MVN \\
                            -Dartifactory.password=${DEPLOY_CREDS_PSW} \\
                            -D skipTests -e

                            #Deploy/Release to maven central repository
                            mvn clean deploy -B -P release -s spring-filters-config/mvn_settings_noproxy.xml \\
                            -D gpg.homedir=/working-dir/spring-filters-config/gnupg \\
                            -D stagingProfileId=$MAVEN_CENTRAL_STAGING_PROFILE_ID \\
                            -D skipTests -e
                        '''
                    }
                    else {
                        echo 'Branch is develop push to MAAXA-MVN-SNAPSHOT'
                        sh '''#!/bin/bash -ex
                            mvn clean deploy -B -s spring-filters-config/mvn_settings_noproxy.xml \\
                            -DaltDeploymentRepository=artifactory.releases::default::https://devcloud.swcoe.ge.com/artifactory/MAAXA-MVN-SNAPSHOT \\
                            -Dartifactory.password=${DEPLOY_CREDS_PSW} \\
                            -D skipTests -e
                        '''
                    }
                }
                // Test reports
                junit '**/surefire-reports/junitreports/TEST*.xml'
                step([$class: 'JacocoPublisher', maximumBranchCoverage: '90', maximumInstructionCoverage: '90'])
=======
        stage('Maven push if develop') {
            when {
                branch 'develop'
            }
            environment {
                DEPLOY_CREDS = credentials('predix-artifactory-uploader')
            }
            steps {
                sh '''#!/bin/bash -ex
                    mvn  -B -s spring-filters-config/mvn_settings_noproxy.xml -DaltDeploymentRepository=artifactory.releases::default::https://devcloud.swcoe.ge.com/artifactory/MAAXA-MVN-SNAPSHOT  -Dartifactory.password=${DEPLOY_CREDS_PSW} clean deploy
                '''
>>>>>>> requested changes
            }
        }
        stage('Maven push if master') {
            when {
                branch 'master'
            }
            environment {
                DEPLOY_CREDS = credentials('predix-artifactory-uploader')
            }
            steps {
                sh '''#!/bin/bash -ex
                    mvn  -B -s spring-filters-config/mvn_settings_noproxy.xml -DaltDeploymentRepository=artifactory.releases::default::https://devcloud.swcoe.ge.com/artifactory/MAAXA-MVN  -Dartifactory.password=${DEPLOY_CREDS_PSW} clean deploy
                    mvn -B deploy -P release -s spring-filters-config/mvn_settings_noproxy.xml -D stagingProfileId=14c243d3be5b9e
                '''
            }
        }
    }
}
