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
                    def dockerImage = docker.build("${IMAGE_NAME}:latest")
                    docker.withRegistry('https://registry.hub.docker.com', 'docker-repo-credential') {
                        dockerImage.push()
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

                            docker compose pull

                            docker compose down
                            docker compose up -d

                            docker image prune -f
                        '
                    """
                }
            }
        }
    }
}