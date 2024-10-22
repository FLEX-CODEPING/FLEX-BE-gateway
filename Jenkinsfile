pipeline {
    agent any

    environment {
        DOCKER_CREDENTIALS = credentials('docker-repo-credential')
        DOCKER_USERNAME = "${DOCKER_CREDENTIALS_USR}"
        GITHUB_TOKEN = credentials('github_access_token')
        REMOTE_USER = 'ubuntu'
        REMOTE_HOST = credentials('gateway-remote-host')
        SSH_CREDENTIALS = credentials('flex-nat-pem')
        IMAGE_NAME = "${DOCKER_USERNAME}/flex-be-gateway"
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
            post {
                success {
                    echo "Successfully Cloned Repository"
                }
                failure {
                    echo "Failed to Clone Repository"
                }
            }
        }

        stage('Build') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew clean assemble -x test'
            }
            post {
                success {
                    echo 'Gradle build success'
                }
                failure {
                    echo 'Gradle build failed'
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    def dockerImage = docker.build("${IMAGE_NAME}:${IMAGE_TAG}")
                    docker.withRegistry('https://registry.hub.docker.com', 'docker-repo-credential') {
                        dockerImage.push()
                        dockerImage.push('latest')
                    }
                }
            }
        }

        stage('Deploy to Remote Server') {
            steps {
                sshagent(credentials: ['flex-nat-pem']) {
                    sh """
                        ssh ${REMOTE_USER}@${REMOTE_HOST} '
                            set -e

                            export IMAGE_TAG=${IMAGE_TAG:-latest}
                            docker compose down --remove-orphans
                            docker compose pull

                            docker compose up -d

                            docker image prune -f
                            docker compose ps
                        '
                    """
                }
            }
        }
    }
}