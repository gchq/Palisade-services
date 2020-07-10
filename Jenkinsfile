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
    image: hadolint/hadolint:latest-debian@sha256:15016b18964c5e623bd2677661a0be3c00ffa85ef3129b11acf814000872861e
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

        stage('Bootstrap') {
            if (env.CHANGE_BRANCH) {
                GIT_BRANCH_NAME = env.CHANGE_BRANCH
            } else {
                GIT_BRANCH_NAME = env.BRANCH_NAME
            }
            echo sh(script: 'env | sort', returnStdout: true)
        }

        stage('Prerequisites') {
            // If this branch name exists in the repo for a mvn dependency
            // Install that version, rather than pulling from nexus
            dir('Palisade-common') {
                git url: 'https://github.com/gchq/Palisade-common.git'
                if (sh(script: "git checkout ${GIT_BRANCH_NAME}", returnStatus: true) == 0) {
                    container('docker-cmds') {
                        configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                            sh 'mvn -s $MAVEN_SETTINGS install -P quick'
                        }
                    }
                }
            }
            dir('Palisade-readers') {
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
            dir('Palisade-services') {
                git url: 'https://github.com/gchq/Palisade-services.git'
                sh "git checkout ${GIT_BRANCH_NAME}"
                container('docker-cmds') {
                    configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                        sh 'mvn -s $MAVEN_SETTINGS install'
                    }
                }
            }
            echo sh(script: 'env | sort', returnStdout: true)
        }

        stage('Integration Tests') {
            // Always run some sort of integration test
            // If this branch name exists in integration-tests, use that
            // Otherwise, default to integration-tests/develop
            dir('Palisade-integration-tests') {
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
            dir('Palisade-services') {
                container('docker-cmds') {
                    withCredentials([string(credentialsId: "${env.SQ_WEB_HOOK}", variable: 'SONARQUBE_WEBHOOK'),
                                     string(credentialsId: "${env.SQ_KEY_STORE_PASS}", variable: 'KEYSTORE_PASS'),
                                     file(credentialsId: "${env.SQ_KEY_STORE}", variable: 'KEYSTORE')]) {
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
            dir('Palisade-services') {
                container('hadolint') {
                    sh 'hadolint */Dockerfile'
                }
            }
        }


        stage('Maven deploy') {
            dir('Palisade-services') {
                container('maven') {
                    configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                        if (("${env.BRANCH_NAME}" == "develop") ||
                                ("${env.BRANCH_NAME}" == "master")) {
                            sh 'palisade-login'
                            //now extract the public IP addresses that this will be open on
                            sh 'extract-addresses'
                            if (sh(script: "namespace-create dev", returnStatus: true) == 0) {
                                sh 'echo namespace create succeeded'
                                sh 'mvn -s $MAVEN_SETTINGS deploy -Dmaven.test.skip=true'
                                //create the branch namespace
                                if (sh(script: "helm upgrade --install palisade . " +
                                        "--set global.hosting=aws  " +
                                        "--set traefik.install=true,dashboard.install=true " +
                                        "--set global.repository=${ECR_REGISTRY} " +
                                        "--set global.hostname=${EGRESS_ELB} " +
                                        "--set global.persistence.classpathJars.aws.volumeHandle=${VOLUME_HANDLE_CLASSPATH_JARS} " +
                                        "--set global.persistence.dataStores.palisade-data-store.aws.volumeHandle=${VOLUME_HANDLE_DATA_STORE} " +
                                        "--set global.persistence.kafka.aws.volumeHandle=${VOLUME_HANDLE_KAFKA} " +
                                        "--set global.persistence.redisCluster.aws.volumeHandle=${VOLUME_HANDLE_REDIS_MASTER} " +
                                        "--set global.persistence.zookeeper.aws.volumeHandle=${VOLUME_HANDLE_ZOOKEEPER} " +
                                        "--set global.redis.install=false " +
                                        "--set global.redis-cluster.install=true " +
                                        "--namespace dev", returnStatus: true) == 0) {
                                    echo("successfully deployed")
                                } else {
                                    error("Build failed because of failed maven deploy")
                                }
                            }
                        } else {
                            //only get the first 10 characters of the namespace
                            def GIT_BRANCH_NAME_LOWER = GIT_BRANCH_NAME.toLowerCase().take(10)
                            sh 'palisade-login'
                            //now extract the public IP addresses that this will be open on
                            sh 'extract-addresses'
                            if (sh(script: "namespace-create ${GIT_BRANCH_NAME_LOWER}", returnStatus: true) == 0) {
                                sh 'echo namespace create succeeded'
                                sh 'mvn -s $MAVEN_SETTINGS install -Dmaven.test.skip=true'
                                //create the branch namespace
                                if (sh(script: "helm upgrade --install palisade . " +
                                        "--set global.hosting=aws  " +
                                        "--set traefik.install=false,dashboard.install=false " +
                                        "--set global.repository=${ECR_REGISTRY} " +
                                        "--set global.hostname=${EGRESS_ELB} " +
                                        "--set global.persistence.classpathJars.aws.volumeHandle=${VOLUME_HANDLE_CLASSPATH_JARS} " +
                                        "--set global.persistence.dataStores.palisade-data-store.aws.volumeHandle=${VOLUME_HANDLE_DATA_STORE} " +
                                        "--set global.persistence.kafka.aws.volumeHandle=${VOLUME_HANDLE_KAFKA} " +
                                        "--set global.persistence.redisCluster.aws.volumeHandle=${VOLUME_HANDLE_REDIS_MASTER} " +
                                        "--set global.persistence.zookeeper.aws.volumeHandle=${VOLUME_HANDLE_ZOOKEEPER} " +
                                        "--set global.redis.install=false " +
                                        "--set global.redis-cluster.install=true " +
                                        "--namespace ${GIT_BRANCH_NAME_LOWER}", returnStatus: true) == 0) {
                                    echo("successfully deployed")
                                } else {
                                    error("Build failed because of failed maven deploy")
                                }
                            } else {
                                error("Could not create namespace")
                            }
                        }
                    }
                }
            }
        }
    }
}
