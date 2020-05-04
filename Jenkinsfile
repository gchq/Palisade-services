/*
 * Copyright 2020 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

podTemplate(yaml: '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: docker-cmds
    image: jnlp-did:jdk11
    imagePullPolicy: IfNotPresent
    command:
    - sleep
    args:
    - 99d
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
  - name: hadolint
    image: hadolint/hadolint:latest-debian@sha256:15016b18964c5e623bd2677661a0be3c00ffa85ef3129b11acf814000872861e
    imagePullPolicy: Always
    command:
        - cat
    tty: true
  - name: docker-daemon
    image: docker:19.03.1-dind
    securityContext:
      privileged: true
    resources:
      requests:
        cpu: 20m
        memory: 512Mi
    volumeMounts:
      - name: docker-graph-storage
        mountPath: /var/lib/docker
    env:
      - name: DOCKER_TLS_CERTDIR
        value: ""

  - name: maven
    image: jnlp-slave-palisade:jdk11
    imagePullPolicy: IfNotPresent
    command: ['cat']
    tty: true
    env:
    - name: TILLER_NAMESPACE
      value: tiller
    - name: HELM_HOST
      value: :44134
    volumeMounts:
      - mountPath: /var/run
        name: docker-sock
  volumes:
    - name: docker-graph-storage
      emptyDir: {}
    - name: docker-sock
      hostPath:
         path: /var/run
''') {
    node(POD_LABEL) {
        def GIT_BRANCH_NAME

        stage('Bootstrap') {
            if (env.CHANGE_BRANCH) {
                GIT_BRANCH_NAME=env.CHANGE_BRANCH
            } else {
                GIT_BRANCH_NAME=env.BRANCH_NAME
            }
            echo sh(script: 'env | sort', returnStdout: true)
        }

        stage('Prerequisites') {
            // If this branch name exists in the repo for a mvn dependency
            // Install that version, rather than pulling from nexus
            dir ('Palisade-common') {
                git url: 'https://github.com/gchq/Palisade-common.git'
                if (sh(script: "git checkout ${GIT_BRANCH_NAME}", returnStatus: true) == 0) {
                    container('docker-cmds') {
                        configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                            sh 'mvn -s $MAVEN_SETTINGS install -P quick'
                        }
                    }
                }
            }
            dir ('Palisade-readers') {
                git url: 'https://github.com/gchq/Palisade-readers.git'
                if (sh(script: "git checkout ${GIT_BRANCH_NAME}", returnStatus: true) == 0) {
                    container('docker-cmds') {
                        configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                            sh 'mvn -s $MAVEN_SETTINGS install -P quick'
                        }
                    }
                }
            }
        }

        stage('Install, Unit Tests, Checkstyle') {
            dir ('Palisade-services') {
                git url: 'https://github.com/gchq/Palisade-services.git'
                sh "git checkout ${GIT_BRANCH_NAME}"
                container('docker-cmds') {
                    configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                        sh 'mvn -s $MAVEN_SETTINGS install'
                    }
                }
            }
        }

        stage('Integration Tests') {
            // Always run some sort of integration test
            // If this branch name exists in integration-tests, use that
            // Otherwise, default to integration-tests/develop
            dir ('Palisade-integration-tests') {
                git url: 'https://github.com/gchq/Palisade-integration-tests.git'
                sh "git checkout ${GIT_BRANCH_NAME} || git checkout develop"
                container('docker-cmds') {
                    configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                        sh 'mvn -s $MAVEN_SETTINGS install'
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            dir ('Palisade-services') {
                container('docker-cmds') {
                    withCredentials([string(credentialsId: '3dc8e0fb-23de-471d-8009-ed1d5890333a', variable: 'SONARQUBE_WEBHOOK'),
                                     string(credentialsId: 'b01b7c11-ccdf-4ac5-b022-28c9b861379a', variable: 'KEYSTORE_PASS'),
                                     file(credentialsId: '91d1a511-491e-4fac-9da5-a61b7933f4f6', variable: 'KEYSTORE')]) {
                        configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                            withSonarQubeEnv(installationName: 'sonar') {
                                sh 'mvn -s $MAVEN_SETTINGS org.sonarsource.scanner.maven:sonar-maven-plugin:3.7.0.1746:sonar -Dsonar.projectKey="Palisade-Services-${BRANCH_NAME}" -Dsonar.projectName="Palisade-Services-${BRANCH_NAME}" -Dsonar.webhooks.project=$SONARQUBE_WEBHOOK -Djavax.net.ssl.trustStore=$KEYSTORE -Djavax.net.ssl.trustStorePassword=$KEYSTORE_PASS'
                            }
                        }
                    }
                }
            }
        }

        stage("SonarQube Quality Gate") {
            // Wait for SonarQube to prepare the report
            sleep(time: 10, unit: 'SECONDS')
            // Just in case something goes wrong, pipeline will be killed after a timeout
            timeout(time: 5, unit: 'MINUTES') {
                // Reuse taskId previously collected by withSonarQubeEnv
                def qg = waitForQualityGate()
                if (qg.status != 'OK') {
                    error "Pipeline aborted due to SonarQube quality gate failure: ${qg.status}"
                }
            }
        }

        stage('Hadolinting') {
            dir ('Palisade-services') {
                container('hadolint') {
                    sh 'hadolint */Dockerfile'
                }
            }
        }

        stage('Maven deploy') {
            dir ('Palisade-services') {
                container('maven') {
                    configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                        if (("${env.BRANCH_NAME}" == "develop") ||
                                ("${env.BRANCH_NAME}" == "master")) {
                            sh 'palisade-login'
                            //now extract the public IP addresses that this will be open on
                            sh 'extract-addresses'
                            sh 'mvn -s $MAVEN_SETTINGS deploy -Dmaven.test.skip=true'
                            sh 'helm upgrade --install palisade . --set traefik.install=true,dashboard.install=true,global.repository=${ECR_REGISTRY},global.hostname=${EGRESS_ELB},global.localMount.enabled=false,global.localMount.volumeHandle=${VOLUME_HANDLE} --namespace dev'
                        } else {
                            sh "echo - no deploy"
                        }
                    }
                }
            }
        }
    }
}
