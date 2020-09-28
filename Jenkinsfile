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


//node-affinity
//nodes 1..3 are reserved for Jenkins slave pods.
//node 0 is used for the Jenkins master
//dind-daemon
//dind-daemon below is the sidecar creation pattern for the docker in docker entity
//it allows the creation and build of docker images
//but more importantly allows the use of testcontainers
//Be careful with upgrading the version number for the dind-daemon
//in experiments carried out, the security priviledges seemed to prevent
//connection to the docker sock for much later versions

timestamps {

    podTemplate(yaml: '''
    apiVersion: v1
    kind: Pod
    metadata:
        name: dind
    spec:
      affinity:
        nodeAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 1
            preference:
              matchExpressions:
              - key: palisade-node-name
                operator: In
                values:
                - node1
                - node2
                - node3
      containers:
      - name: jnlp
        image: jenkins/jnlp-slave
        imagePullPolicy: Always
        args:
        - $(JENKINS_SECRET)
        - $(JENKINS_NAME)
        resources:
          requests:
            ephemeral-storage: "4Gi"
          limits:
            ephemeral-storage: "8Gi"

      - name: docker-cmds
        image: 779921734503.dkr.ecr.eu-west-1.amazonaws.com/jnlp-did:200608
        imagePullPolicy: IfNotPresent
        command:
        - sleep
        args:
        - 99d
        env:
          - name: DOCKER_HOST
            value: tcp://localhost:2375
        resources:
          requests:
            ephemeral-storage: "4Gi"
          limits:
            ephemeral-storage: "8Gi"

      - name: hadolint
        image: hadolint/hadolint:v1.18.0-6-ga0d655d-alpine@sha256:e0f960b5acf09ccbf092ec1e8f250cd6b5c9a586a0e9783c53618d76590b6aec
        imagePullPolicy: IfNotPresent
        command:
            - cat
        tty: true
        resources:
          requests:
            ephemeral-storage: "1Gi"
          limits:
            ephemeral-storage: "2Gi"

      - name: dind-daemon
        image: docker:1.12.6-dind
        imagePullPolicy: IfNotPresent
        resources:
          requests:
            cpu: 20m
            memory: 512Mi
        securityContext:
          privileged: true
        volumeMounts:
          - name: docker-graph-storage
            mountPath: /var/lib/docker
        resources:
          requests:
            ephemeral-storage: "1Gi"
          limits:
            ephemeral-storage: "2Gi"

      - name: maven
        image: 779921734503.dkr.ecr.eu-west-1.amazonaws.com/jnlp-dood-new-infra:200710
        imagePullPolicy: IfNotPresent
        command: ['docker', 'run', '-p', '80:80', 'httpd:latest']
        tty: true
        volumeMounts:
          - mountPath: /var/run
            name: docker-sock
        resources:
          requests:
            ephemeral-storage: "4Gi"
          limits:
            ephemeral-storage: "8Gi"

      volumes:
        - name: docker-graph-storage
          emptyDir: {}
        - name: docker-sock
          hostPath:
             path: /var/run

    ''') {
        node(POD_LABEL) {
            def GIT_BRANCH_NAME
            def GIT_BRANCH_NAME_LOWER
            def SERVICES_REVISION
            def COMMON_REVISION
            def READERS_REVISION
            def CLIENTS_REVISION
            def EXAMPLES_REVISION
            def INTEGRATION_REVISION
            def HELM_DEPLOY_NAMESPACE
            def IS_PR
            def FEATURE_BRANCH

            stage('Bootstrap') {
                if (env.CHANGE_BRANCH) {
                    GIT_BRANCH_NAME = env.CHANGE_BRANCH
                    IS_PR = "true"
                } else {
                    GIT_BRANCH_NAME = env.BRANCH_NAME
                    IS_PR = "false"
                }
                // set default values for the variables
                FEATURE_BRANCH = "true"
                COMMON_REVISION = "SNAPSHOT"
                READERS_REVISION = "SNAPSHOT"
                CLIENTS_REVISION = "SNAPSHOT"
                EXAMPLES_REVISION = "SNAPSHOT"
                INTEGRATION_REVISION = "SNAPSHOT"
                GIT_BRANCH_NAME_LOWER = GIT_BRANCH_NAME.toLowerCase().take(7)
                SERVICES_REVISION = "BRANCH-${GIT_BRANCH_NAME_LOWER}-SNAPSHOT"
                HELM_DEPLOY_NAMESPACE = GIT_BRANCH_NAME_LOWER
                // update values for the variables if this is the develop branch build
                if ("${env.BRANCH_NAME}" == "develop") {
                    SERVICES_REVISION = "SNAPSHOT"
                    HELM_DEPLOY_NAMESPACE = "dev"
                    FEATURE_BRANCH = "false"
                }
                // update values for the variables if this is the main branch build
                if ("${env.BRANCH_NAME}" == "main") {
                    SERVICES_REVISION = "RELEASE"
                    COMMON_REVISION = "RELEASE"
                    READERS_REVISION = "RELEASE"
                    CLIENTS_REVISION = "RELEASE"
                    EXAMPLES_REVISION = "RELEASE"
                    INTEGRATION_REVISION = "RELEASE"
                    HELM_DEPLOY_NAMESPACE = "release"
                    FEATURE_BRANCH = "false"
                }
                echo sh(script: 'env | sort', returnStdout: true)
            }

            stage('Prerequisites') {
                container('docker-cmds') {
                    configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                        if (FEATURE_BRANCH == "true") {
                            dir('Palisade-common') {
                                git branch: 'develop', url: 'https://github.com/gchq/Palisade-common.git'
                                if (sh(script: "git checkout ${GIT_BRANCH_NAME}", returnStatus: true) == 0) {
                                    COMMON_REVISION = "BRANCH-${GIT_BRANCH_NAME_LOWER}-SNAPSHOT"
                                }
                            }

                            dir('Palisade-readers') {
                                git branch: 'develop', url: 'https://github.com/gchq/Palisade-readers.git'
                                if (sh(script: "git checkout ${GIT_BRANCH_NAME}", returnStatus: true) == 0) {
                                    READERS_REVISION = "BRANCH-${GIT_BRANCH_NAME_LOWER}-SNAPSHOT"
                                } else {
                                     if (COMMON_REVISION == "BRANCH-${GIT_BRANCH_NAME_LOWER}-SNAPSHOT") {
                                         READERS_REVISION = "BRANCH-${GIT_BRANCH_NAME_LOWER}-SNAPSHOT"
                                         sh "mvn -s ${MAVEN_SETTINGS} -P quick -D revision=${READERS_REVISION} -D common.revision=${COMMON_REVISION} deploy"
                                     }
                                }
                            }

                            dir('Palisade-clients') {
                                git branch: 'develop', url: 'https://github.com/gchq/Palisade-clients.git'
                                if (sh(script: "git checkout ${GIT_BRANCH_NAME}", returnStatus: true) == 0) {
                                    CLIENTS_REVISION = "BRANCH-${GIT_BRANCH_NAME_LOWER}-SNAPSHOT"
                                } else {
                                     if (READERS_REVISION == "BRANCH-${GIT_BRANCH_NAME_LOWER}-SNAPSHOT") {
                                         CLIENTS_REVISION = "BRANCH-${GIT_BRANCH_NAME_LOWER}-SNAPSHOT"
                                         sh "mvn -s ${MAVEN_SETTINGS} -P quick -D revision=${CLIENTS_REVISION} -D common.revision=${COMMON_REVISION} -D readers.revision=${READERS_REVISION} deploy"
                                     }
                                }
                            }

                            dir('Palisade-examples') {
                                git branch: 'develop', url: 'https://github.com/gchq/Palisade-examples.git'
                                if (sh(script: "git checkout ${GIT_BRANCH_NAME}", returnStatus: true) == 0) {
                                    EXAMPLES_REVISION = "BRANCH-${GIT_BRANCH_NAME_LOWER}-SNAPSHOT"
                                } else {
                                    if (CLIENTS_REVISION == "BRANCH-${GIT_BRANCH_NAME_LOWER}-SNAPSHOT") {
                                        EXAMPLES_REVISION = "BRANCH-${GIT_BRANCH_NAME_LOWER}-SNAPSHOT"
                                        sh "mvn -s ${MAVEN_SETTINGS} -P quick -D revision=${EXAMPLES_REVISION} -D common.revision=${COMMON_REVISION} -D readers.revision=${READERS_REVISION} -D clients.revision=${CLIENTS_REVISION} deploy"
                                    }
                                }
                            }
                        }
                    }
                }
            }

            parallel Test: {
                stage('Install, Unit Tests, Checkstyle') {
                    container('docker-cmds') {
                        configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                            dir('Palisade-services') {
                                git branch: GIT_BRANCH_NAME, url: 'https://github.com/gchq/Palisade-services.git'
                                // only going to install here as the deploy will happen in the deploy parallel build if it is not a feature branch or if it is a PR build
                                sh "mvn -s ${MAVEN_SETTINGS} -D dockerfile.skip=true -D revision=${SERVICES_REVISION} -D common.revision=${COMMON_REVISION} -D readers.revision=${READERS_REVISION} -D examples.revision=${EXAMPLES_REVISION} install"
                            }
                        }
                    }
                }

                stage('SonarQube Analysis') {
                    container('docker-cmds') {
                        withCredentials([string(credentialsId: "${env.SQ_WEB_HOOK}", variable: 'SONARQUBE_WEBHOOK'),
                                         string(credentialsId: "${env.SQ_KEY_STORE_PASS}", variable: 'KEYSTORE_PASS'),
                                         file(credentialsId: "${env.SQ_KEY_STORE}", variable: 'KEYSTORE')]) {
                            configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                                withSonarQubeEnv(installationName: 'sonar') {
                                    dir('Palisade-services') {
                                        sh "mvn -s ${MAVEN_SETTINGS} org.sonarsource.scanner.maven:sonar-maven-plugin:3.7.0.1746:sonar -Dsonar.projectKey=Palisade-Services-${GIT_BRANCH_NAME} -Dsonar.projectName=Palisade-Services-${GIT_BRANCH_NAME} -Dsonar.webhooks.project=$SONARQUBE_WEBHOOK -Djavax.net.ssl.trustStore=$KEYSTORE -Djavax.net.ssl.trustStorePassword=$KEYSTORE_PASS"
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
                    container('hadolint') {
                        dir('Palisade-services') {
                            sh 'hadolint */Dockerfile'
                        }
                    }
                }
            },

            Deploy: {
                stage('Helm deploy') {
                    container('maven') {
                        configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                            dir('Palisade-services-deploy') {
                                if (IS_PR == "true" || FEATURE_BRANCH == "false") {
                                    git branch: GIT_BRANCH_NAME, url: 'https://github.com/gchq/Palisade-services.git'
                                    sh 'palisade-login'
                                    //now extract the public IP addresses that this will be open on
                                    sh 'extract-addresses'
                                    // Push containers to the registry so they are available to helm
                                    if (FEATURE_BRANCH == "true") {
                                        if (sh(script: "namespace-create ${GIT_BRANCH_NAME_LOWER}", returnStatus: true) == 0) {
                                            sh 'echo namespace create succeeded'
                                        } else {
                                            error("Could not create namespace")
                                        }
                                    }
                                    sh "mvn -s ${MAVEN_SETTINGS} -P pi -D maven.test.skip=true -D revision=${SERVICES_REVISION} -D common.revision=${COMMON_REVISION} -D readers.revision=${READERS_REVISION} -D examples.revision=${EXAMPLES_REVISION} deploy"
                                    //deploy application to the cluster
                                    if (sh(script: "helm upgrade --install palisade . " +
                                            "--set global.hosting=aws  " +
                                            "--set traefik.install=false,dashboard.install=false " +
                                            "--set global.repository=${ECR_REGISTRY} " +
                                            "--set global.hostname=${EGRESS_ELB} " +
                                            "--set global.deployment=example " +
                                            "--set global.persistence.dataStores.palisade-data-store.aws.volumeHandle=${VOLUME_HANDLE_DATA_STORE} " +
                                            "--set global.persistence.classpathJars.aws.volumeHandle=${VOLUME_HANDLE_CLASSPATH_JARS} " +
                                            "--set global.redisClusterEnabled=true " +
                                            "--set global.redis.install=false " +
                                            "--set global.redis-cluster.install=true " +
                                            "--namespace ${HELM_DEPLOY_NAMESPACE}", returnStatus: true) == 0) {
                                        echo("successfully deployed")
                                    } else {
                                        error("Build failed because of failed maven deploy")
                                    }
                                    if (FEATURE_BRANCH == "true") {
                                        sh "helm delete palisade --namespace ${HELM_DEPLOY_NAMESPACE}"
                                        sh "kubectl delete pvc --all --namespace ${HELM_DEPLOY_NAMESPACE}"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}