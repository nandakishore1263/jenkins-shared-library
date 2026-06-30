def call(Map config = [:]) {

    // get values from config or use defaults
    def mavenVersion = config.mavenVersion ?: 'Maven-3.9'
    def appName      = config.appName      ?: 'my-app'
    def email        = config.email        ?: 'default@gmail.com'

    pipeline {
        agent any

        tools {
            maven "${mavenVersion}"
        }

        environment {
            APP_NAME = "${appName}"
            VERSION  = '1.0-SNAPSHOT'
        }

        stages {

            stage('Checkout') {
                steps {
                    echo "Starting ${APP_NAME} build #${BUILD_NUMBER}"
                    checkout scm
                }
            }

            stage('Compile') {
                steps {
                    echo "Compiling ${APP_NAME}..."
                    sh 'mvn compile'
                }
            }

            stage('Test') {
                steps {
                    echo "Testing ${APP_NAME}..."
                    sh 'mvn test'
                }
                post {
                    always {
                        junit 'target/surefire-reports/*.xml'
                    }
                }
            }

            stage('Package') {
                steps {
                    echo "Packaging ${APP_NAME}..."
                    sh 'mvn package -DskipTests'
                }
            }

            stage('Archive') {
                steps {
                    archiveArtifacts artifacts: 'target/*.jar',
                                     fingerprint: true
                }
            }
        }

        post {
            success {
                echo "✅ ${APP_NAME} build #${BUILD_NUMBER} passed!"
                emailext(
                    subject: "✅ SUCCESS: ${APP_NAME} #${BUILD_NUMBER}",
                    body: """
                        Build SUCCESSFUL! ✅
                        Project : ${APP_NAME}
                        Build # : ${BUILD_NUMBER}
                        URL     : ${BUILD_URL}
                        Regards,
                        Jenkins
                    """,
                    to: "${email}"
                )
            }
            failure {
                echo "❌ ${APP_NAME} build #${BUILD_NUMBER} failed!"
                emailext(
                    subject: "❌ FAILED: ${APP_NAME} #${BUILD_NUMBER}",
                    body: """
                        Build FAILED! ❌
                        Project : ${APP_NAME}
                        Build # : ${BUILD_NUMBER}
                        URL     : ${BUILD_URL}console
                        Regards,
                        Jenkins
                    """,
                    to: "${email}"
                )
            }
            always {
                echo 'Pipeline finished.'
            }
        }
    }
}
