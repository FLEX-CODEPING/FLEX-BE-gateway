pipeline {
    agent any

    environment {
        DOCKER_CREDENTIALS = credentials('docker-repo-credential')
        K8S_NAMESPACE = 'default'
        DOCKER_USERNAME = "${DOCKER_CREDENTIALS_USR}"
        GITHUB_TOKEN = credentials('github_access_token')
        SSH_HOST = '210.109.52.73'
        SSH_PORT = '10000'
    }

    triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'ref', value: '$.ref']
            ],
            causeString: 'Triggered on $ref',
            token: env.GITHUB_TOKEN,
            printContributedVariables: true,
            printPostContent: true,
            silentResponse: false,
            regexpFilterText: '$ref',
            regexpFilterExpression: '^refs/heads/develop$'
        )
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
                    def dockerImage = docker.build("${DOCKER_USERNAME}/flex-be-gateway:${BUILD_NUMBER}")
                    docker.withRegistry('https://registry.hub.docker.com', 'docker-repo-credential') {
                        dockerImage.push()
                    }
                }
            }
        }

        stage('Create or Update ConfigMap') {
            steps {
                withCredentials([
                    file(credentialsId: 'GATEWAY_APPLICATION_YML', variable: 'APPLICATION_YML'),
                    file(credentialsId: 'GATEWAY_YML', variable: 'GATEWAY_YML'),
                    file(credentialsId: 'GATEWAY_SWAGGER_YML', variable: 'SWAGGER_YML'),
                    file(credentialsId: 'GATEWAY_AUTH_YML', variable: 'AUTH_YML')
                ]) {
                    sh '''
                        kubectl create configmap gateway-config \
                        --from-literal=APPLICATION_PROFILE=dev \
                        --from-literal=EUREKA_SERVER_URL=http://eureka-service:8761/eureka/ \
                        --from-file=application.yml="${APPLICATION_YML}" \
                        --from-file=gateway.yml="${GATEWAY_YML}" \
                        --from-file=swagger.yml="${SWAGGER_YML}" \
                        --from-file=auth.yml="${AUTH_YML}" \
                        -n default --dry-run=client -o yaml | kubectl apply -f -
                    '''
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    def deploymentYaml = readFile("gateway-dev-deployment.yaml")
                    deploymentYaml = deploymentYaml.replaceAll('\\$\\{DOCKER_USERNAME\\}', DOCKER_CREDENTIALS_USR)
                    deploymentYaml = deploymentYaml.replaceAll('\\$\\{IMAGE_TAG\\}', BUILD_NUMBER)

                    writeFile file: 'temp-deployment.yaml', text: deploymentYaml

                    sshagent(credentials: ['flex-nat-pem']) {
                        sh """
                            scp -P ${SSH_PORT} -o StrictHostKeyChecking=no temp-deployment.yaml ubuntu@${SSH_HOST}:/tmp/temp-deployment.yaml
                            ssh -p ${SSH_PORT} -o StrictHostKeyChecking=no ubuntu@${SSH_HOST} '
                                kubectl apply -f /tmp/temp-deployment.yaml -n ${K8S_NAMESPACE} &&
                                rm /tmp/temp-deployment.yaml
                            '
                        """
                    }
                }
            }
        }
    }
}
