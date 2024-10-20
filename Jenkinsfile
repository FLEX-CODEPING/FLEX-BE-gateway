pipeline {
    agent any

    environment {
        DOCKER_CREDENTIALS = credentials('docker-repo-credential')
        DOCKER_USERNAME = "${DOCKER_CREDENTIALS_USR}"
        GITHUB_TOKEN = credentials('github_access_token')
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
                    def dockerImage = docker.build("${DOCKER_USERNAME}/flex-be-gateway:latest")
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
                        ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} -p ${SSH_PORT} '
                            docker pull ${DOCKER_USERNAME}/flex-be-gateway:latest
                            docker stop flex-be-gateway || true
                            docker rm flex-be-gateway || true
                            docker run -d --name flex-be-gateway -p 8080:8080 ${DOCKER_USERNAME}/flex-be-gateway:latest
                        '
                    """
                }
            }
        }
    }
}