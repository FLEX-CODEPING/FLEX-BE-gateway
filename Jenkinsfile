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
        SLACK_CHANNEL = '#backend-jenkins'
        IMAGE_NAME = "${DOCKER_USERNAME}/flex-be-gateway"
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    checkout scm
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    slackSend(channel: SLACK_CHANNEL, message: "🏗️ GATEWAY Build #${env.BUILD_NUMBER} is starting...")
                    sh 'chmod +x gradlew'
                    sh './gradlew clean assemble -x test'
                }
            }
            post {
                success {
                    echo 'Gradle build success'
                    slackSend(channel: SLACK_CHANNEL, message: "✅ GATEWAY build succeeded for Build #${env.BUILD_NUMBER}.")
                }
                failure {
                    echo 'Gradle build failed'
                    slackSend(channel: SLACK_CHANNEL, message: "⛔️ GATEWAY build failed for Build #${env.BUILD_NUMBER}.")
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
                    slackSend(channel: SLACK_CHANNEL, message: "🐳 Docker image built and pushed for Build #${env.BUILD_NUMBER}.")
                }
            }
        }

        stage('Update Helm Values YAML') {
            steps {
                script {
                    slackSend(channel: SLACK_CHANNEL, message: "🔄 Updating Helm values for Build #${env.BUILD_NUMBER}...")
                    git branch: 'main', credentialsId: 'github-signin', url: 'https://github.com/FLEX-CODEPING/FLEX-CD.git'
                    sh """
                    sed -i 's|tag: .*|tag: ${IMAGE_TAG}|' charts/auth-service/values.yaml
                    """
                    withCredentials([usernamePassword(credentialsId: 'github-signin', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
                        sh """
                        git config user.email "kiwijiomn@gmail.com"
                        git config user.name "kiwijiomn"
                        git add charts/auth-service/values.yaml
                        git commit -m "[UPDATE] auth-service image tag ${IMAGE_TAG}"
                        git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/KEA-Lucky7/MOABOA-GitOps.git main
                        """
                    }
                    slackSend(channel: SLACK_CHANNEL, message: "✅ Helm values.yaml updated for Build #${env.BUILD_NUMBER}.")
                }
            }
        }
    }
}