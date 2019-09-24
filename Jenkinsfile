/*
 * Copyright 2019 Crown Copyright
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
    image: ${env.DOCKER_IMAGE}
    command:
    - sleep
    args:
    - 99d
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
        
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
    image: ${env.INFRA_IMAGE}
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
        stage('Install a Maven project') {
            git branch: "${env.BRANCH_NAME}", url: 'https://github.com/gchq/Palisade-services.git'
            container('docker-cmds') {
                configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                    sh 'mvn -s $MAVEN_SETTINGS install'
                }
            }
        }
        stage('Deploy a Maven project') {
            git branch: "${env.BRANCH_NAME}", url: 'https://github.com/gchq/Palisade-services.git'
            container('maven') {
                configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                    sh 'palisade-login'
                    sh 'mvn -s $MAVEN_SETTINGS deploy -Dmaven.test.skip=true'
                }
            }
        }
    }
}
