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
    imagePullPolicy: Never
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
    imagePullPolicy: Never
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
        stage('Bootstrap') {
            echo sh(script: 'env|sort', returnStdout: true)
        }

        stage('Integration Tests') {
        echo sh(script: '''
            x = "develop"
            git clone https://github.com/gchq/Palisade-integration-tests.git
            cd Palisade-integration-tests
            if git show-ref --quiet refs/heads/${env.BRANCH_NAME}; then
                x = ${env.BRANCH_NAME}
                echo develop branch exists
            fi
            echo stuff did things and others
        ''', returnStdout: true)
            container('docker-cmds') {
                configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                    sh 'mvn -s $MAVEN_SETTINGS install -Dmaven.test.skip=true'
                }
            }
        }
        stage('Maven deploy') {
            x = env.BRANCH_NAME

            if (x.substring(0, 2) == "PR") {
                y = x.substring(3)
                git url: 'https://github.com/gchq/Palisade-services.git'
                sh "git fetch origin pull/${y}/head:${x}"
                sh "git checkout ${x}"
            } else { //just a normal branch
                git branch: "${env.BRANCH_NAME}", url: 'https://github.com/gchq/Palisade-services.git'
            }
            container('maven') {
                configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                    if (("${env.BRANCH_NAME}" == "develop") ||
                            ("${env.BRANCH_NAME}" == "master")) {
                        sh 'palisade-login'
                        //now extract the public IP addresses that this will be open on
                        sh 'extract-addresses'
                        sh 'mvn -s $MAVEN_SETTINGS deploy -Dmaven.test.skip=true'
                        sh 'helm upgrade --install palisade . --set traefik.install=true,dashboard.install=true --set global.repository=${ECR_REGISTRY}  --set global.hostname=${EGRESS_ELB} --namespace dev'
                    } else {
                        sh "echo - no deploy"
                    }
                }
            }
        }
    }
    // No need to occupy a node
            stage("SonarQube Quality Gate"){
              timeout(time: 1, unit: 'HOURS') { // Just in case something goes wrong, pipeline will be killed after a timeout
                def qg = waitForQualityGate() // Reuse taskId previously collected by withSonarQubeEnv
                if (qg.status != 'OK') {
                  error "Pipeline aborted due to SonarQube quality gate failure: ${qg.status}"
                }
              }
            }
}
