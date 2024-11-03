pipeline {
    agent any

    environment {
        DOCKER_CREDENTIALS = credentials('docker-repo-credential')
        DOCKER_USERNAME = "${DOCKER_CREDENTIALS_USR}"
        GITHUB_TOKEN = credentials('github-access-token')
        SSH_CREDENTIALS = credentials('flex-server-pem')
        REMOTE_USER = credentials('remote-user')
        BASTION_HOST = credentials('bastion-host')
        REMOTE_HOST = credentials('dev-gateway-host')
        SLACK_CHANNEL = '#backend-jenkins'  // 채널 이름 수정
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
             script {
                   slackSend(channel: SLACK_CHANNEL, message: "🏗️ GATEWAY Build #${env.BUILD_NUMBER} is starting...")
             }
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew clean assemble -x test'
            }
            post {
                success {
                    echo 'Gradle build success'
                    slackSend(channel: SLACK_CHANNEL, message: "✅ Gradle build succeeded for Build #${env.BUILD_NUMBER}.")
                }
                failure {
                    echo 'Gradle build failed'
                    slackSend(channel: SLACK_CHANNEL, message: "⛔️ Gradle build failed for Build #${env.BUILD_NUMBER}.")
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
                sshagent(credentials: ['flex-server-pem']) {  // PEM 키를 사용하여 SSH 인증
                    sh """
                        ssh -J ${REMOTE_USER}@${BASTION_HOST} ${REMOTE_USER}@${REMOTE_HOST} '
                            set -e

                            # 환경 변수 설정
                            export IMAGE_TAG=${IMAGE_TAG}

                            docker compose down --remove-orphans

                            # Docker Compose 파일에 IMAGE_TAG 적용
                            sed -i "s|image: ${IMAGE_NAME}:.*|image: ${IMAGE_NAME}:${IMAGE_TAG}|" docker-compose.yml

                            docker compose pull
                            docker compose up -d

                            docker image prune -f

                            docker compose ps
                        '
                    """
                }
            }
            post {
                success {
                    slackSend(channel: SLACK_CHANNEL, message: "🚀 Deployment SUCCEED for Build #${env.BUILD_NUMBER}.")
                }
                failure {
                    slackSend(channel: SLACK_CHANNEL, message: "⛔️ Deployment FAILED for Build #${env.BUILD_NUMBER}.")
                }
            }
        }
    }

}